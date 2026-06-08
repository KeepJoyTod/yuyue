package com.amberfilm.member;

import com.amberfilm.common.ApiException;
import com.amberfilm.member.MemberDtos.AdjustCardTimesRequest;
import com.amberfilm.member.MemberDtos.AdjustPointsRequest;
import com.amberfilm.member.MemberDtos.AdjustWalletRequest;
import com.amberfilm.member.MemberDtos.AdminMemberAssetsDto;
import com.amberfilm.member.MemberDtos.GrantCardRequest;
import com.amberfilm.member.MemberDtos.GrantCouponRequest;
import com.amberfilm.member.MemberDtos.MemberAssetSummaryDto;
import com.amberfilm.member.MemberDtos.MemberCardDto;
import com.amberfilm.member.MemberDtos.MemberCouponDto;
import com.amberfilm.member.MemberDtos.PointTransactionDto;
import com.amberfilm.member.MemberDtos.WalletTransactionDto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberAssetService {
  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 100;

  private final JdbcTemplate jdbcTemplate;

  public MemberAssetService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public MemberAssetSummaryDto summary(long userId) {
    return new MemberAssetSummaryDto(
        count("""
            SELECT COUNT(*)
            FROM member_coupons
            WHERE user_id = ? AND status = 'available'
              AND valid_from <= CURRENT_TIMESTAMP AND expires_at >= CURRENT_TIMESTAMP
            """, userId),
        intValue("SELECT COALESCE(balance, 0) FROM member_point_accounts WHERE user_id = ?", userId),
        intValue("SELECT COALESCE(balance_cent, 0) FROM member_wallet_accounts WHERE user_id = ?", userId),
        count("""
            SELECT COUNT(*)
            FROM member_cards
            WHERE user_id = ? AND status = 'valid' AND remaining_times > 0
              AND valid_from <= CURRENT_TIMESTAMP
              AND (expires_at IS NULL OR expires_at >= CURRENT_TIMESTAMP)
            """, userId));
  }

  public List<MemberCouponDto> coupons(long userId, String status) {
    String filter = normalizeCouponFilter(status);
    StringBuilder sql = new StringBuilder(couponSql()).append(" WHERE c.user_id = ?");
    if ("available".equals(filter)) {
      sql.append("""
           AND c.status = 'available'
           AND c.valid_from <= CURRENT_TIMESTAMP
           AND c.expires_at >= CURRENT_TIMESTAMP
          """);
    } else if ("expired".equals(filter)) {
      sql.append(" AND c.status = 'available' AND c.expires_at < CURRENT_TIMESTAMP");
    } else if (!"all".equals(filter)) {
      sql.append(" AND c.status = ?");
      return jdbcTemplate.query(sql.append(" ORDER BY c.created_at DESC").toString(), couponMapper(), userId, filter);
    }
    sql.append(" ORDER BY c.created_at DESC");
    return jdbcTemplate.query(sql.toString(), couponMapper(), userId);
  }

  public List<MemberCardDto> cards(long userId, String status) {
    String filter = normalizeCardFilter(status);
    StringBuilder sql = new StringBuilder(cardSql()).append(" WHERE c.user_id = ?");
    if ("valid".equals(filter)) {
      sql.append("""
           AND c.status = 'valid'
           AND c.remaining_times > 0
           AND c.valid_from <= CURRENT_TIMESTAMP
           AND (c.expires_at IS NULL OR c.expires_at >= CURRENT_TIMESTAMP)
          """);
    } else if ("expired".equals(filter)) {
      sql.append(" AND c.status = 'valid' AND c.expires_at IS NOT NULL AND c.expires_at < CURRENT_TIMESTAMP");
    } else if (!"all".equals(filter)) {
      sql.append(" AND c.status = ?");
      return jdbcTemplate.query(sql.append(" ORDER BY c.created_at DESC").toString(), cardMapper(), userId, filter);
    }
    sql.append(" ORDER BY c.created_at DESC");
    return jdbcTemplate.query(sql.toString(), cardMapper(), userId);
  }

  public List<PointTransactionDto> pointTransactions(long userId, Integer limit) {
    return jdbcTemplate.query("""
        SELECT id, type, delta_points, balance_after, source, source_order_id, reason, created_at
        FROM member_point_transactions
        WHERE user_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT ?
        """, pointTransactionMapper(), userId, normalizeLimit(limit));
  }

  public List<WalletTransactionDto> walletTransactions(long userId, Integer limit) {
    return jdbcTemplate.query("""
        SELECT id, type, delta_cent, balance_after_cent, source, source_order_id, reason, created_at
        FROM member_wallet_transactions
        WHERE user_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT ?
        """, walletTransactionMapper(), userId, normalizeLimit(limit));
  }

  public AdminMemberAssetsDto adminAssets(long userId) {
    requireUser(userId);
    return new AdminMemberAssetsDto(
        summary(userId),
        coupons(userId, "all"),
        cards(userId, "all"),
        pointTransactions(userId, DEFAULT_LIMIT),
        walletTransactions(userId, DEFAULT_LIMIT));
  }

  @Transactional
  public MemberCouponDto grantCoupon(String adminTokenDigest, long userId, GrantCouponRequest request) {
    requireUser(userId);
    String title = requireText(request.title(), "优惠券名称不能为空");
    String couponType = normalizeCouponType(request.couponType());
    int amountCent = requirePositive(request.amountCent(), "优惠券金额必须大于 0");
    int thresholdCent = request.thresholdCent() == null ? 0 : request.thresholdCent();
    if (thresholdCent < 0) {
      throw ApiException.badRequest("VALIDATION_ERROR", "使用门槛不能小于 0");
    }
    Timestamp validFrom = optionalTimestamp(request.validFrom());
    if (validFrom == null) {
      validFrom = new Timestamp(System.currentTimeMillis());
    }
    Timestamp expiresAt = requireTimestamp(request.expiresAt(), "优惠券过期时间不能为空");
    if (expiresAt.before(validFrom)) {
      throw ApiException.badRequest("VALIDATION_ERROR", "优惠券过期时间不能早于生效时间");
    }
    String reason = blankToNull(request.reason());
    KeyHolder keyHolder = new GeneratedKeyHolder();
    Timestamp finalValidFrom = validFrom;
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO member_coupons(
            user_id, title, coupon_type, amount_cent, threshold_cent, valid_from, expires_at, source, reason)
          VALUES (?, ?, ?, ?, ?, ?, ?, 'admin', ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setString(2, title);
      ps.setString(3, couponType);
      ps.setInt(4, amountCent);
      ps.setInt(5, thresholdCent);
      ps.setTimestamp(6, finalValidFrom);
      ps.setTimestamp(7, expiresAt);
      ps.setString(8, reason);
      return ps;
    }, keyHolder);
    long couponId = generatedId(keyHolder);
    writeAuditLog(adminTokenDigest, "COUPON_GRANT", "coupon", couponId,
        "{\"userId\":%d,\"amountCent\":%d,\"reason\":%s}".formatted(userId, amountCent, jsonValue(reason)));
    return couponById(couponId);
  }

  @Transactional
  public MemberCouponDto voidCoupon(String adminTokenDigest, long couponId, String reason) {
    Map<String, Object> coupon = requireCouponForUpdate(couponId);
    String status = (String) coupon.get("status");
    if (!"available".equals(status)) {
      throw ApiException.badRequest("COUPON_STATUS_INVALID", "只有可用优惠券可以作废");
    }
    jdbcTemplate.update("""
        UPDATE member_coupons
        SET status = 'voided', reason = COALESCE(?, reason), updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, blankToNull(reason), couponId);
    writeAuditLog(adminTokenDigest, "COUPON_VOID", "coupon", couponId,
        "{\"reason\":%s}".formatted(jsonValue(reason)));
    return couponById(couponId);
  }

  @Transactional
  public PointTransactionDto adjustPoints(String adminTokenDigest, long userId, AdjustPointsRequest request) {
    requireUser(userId);
    int delta = requireDelta(request.deltaPoints(), "积分调整值不能为 0");
    long transactionId = adjustPointBalance(userId, delta, "admin_adjust", "admin", null, blankToNull(request.reason()));
    writeAuditLog(adminTokenDigest, "POINT_ADJUST", "user", userId,
        "{\"deltaPoints\":%d,\"reason\":%s}".formatted(delta, jsonValue(request.reason())));
    return pointTransactionById(transactionId);
  }

  @Transactional
  public WalletTransactionDto adjustWallet(String adminTokenDigest, long userId, AdjustWalletRequest request) {
    requireUser(userId);
    int delta = requireDelta(request.deltaCent(), "余额调整值不能为 0");
    long transactionId = adjustWalletBalance(userId, delta, "admin_adjust", "admin", null, blankToNull(request.reason()));
    writeAuditLog(adminTokenDigest, "WALLET_ADJUST", "user", userId,
        "{\"deltaCent\":%d,\"reason\":%s}".formatted(delta, jsonValue(request.reason())));
    return walletTransactionById(transactionId);
  }

  @Transactional
  public MemberCardDto grantCard(String adminTokenDigest, long userId, GrantCardRequest request) {
    requireUser(userId);
    String title = requireText(request.title(), "会员卡名称不能为空");
    if (request.applicableServiceId() != null) {
      requireExists("SELECT COUNT(*) FROM services WHERE id = ?", request.applicableServiceId(), "适用套餐不存在");
    }
    int totalTimes = requirePositive(request.totalTimes(), "次卡次数必须大于 0");
    Timestamp validFrom = optionalTimestamp(request.validFrom());
    if (validFrom == null) {
      validFrom = new Timestamp(System.currentTimeMillis());
    }
    Timestamp expiresAt = optionalTimestamp(request.expiresAt());
    if (expiresAt != null && expiresAt.before(validFrom)) {
      throw ApiException.badRequest("VALIDATION_ERROR", "次卡过期时间不能早于生效时间");
    }
    String reason = blankToNull(request.reason());
    KeyHolder keyHolder = new GeneratedKeyHolder();
    Timestamp finalValidFrom = validFrom;
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO member_cards(
            user_id, title, applicable_service_id, total_times, remaining_times, valid_from, expires_at, source, reason)
          VALUES (?, ?, ?, ?, ?, ?, ?, 'admin', ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setString(2, title);
      if (request.applicableServiceId() == null) {
        ps.setObject(3, null);
      } else {
        ps.setLong(3, request.applicableServiceId());
      }
      ps.setInt(4, totalTimes);
      ps.setInt(5, totalTimes);
      ps.setTimestamp(6, finalValidFrom);
      ps.setTimestamp(7, expiresAt);
      ps.setString(8, reason);
      return ps;
    }, keyHolder);
    long cardId = generatedId(keyHolder);
    insertCardTransaction(cardId, userId, "grant", totalTimes, totalTimes, "admin", reason);
    writeAuditLog(adminTokenDigest, "CARD_GRANT", "card", cardId,
        "{\"userId\":%d,\"totalTimes\":%d,\"reason\":%s}".formatted(userId, totalTimes, jsonValue(reason)));
    return cardById(cardId);
  }

  @Transactional
  public MemberCardDto adjustCardTimes(String adminTokenDigest, long cardId, AdjustCardTimesRequest request) {
    int delta = requireDelta(request.deltaTimes(), "次卡调整次数不能为 0");
    Map<String, Object> card = requireCardForUpdate(cardId);
    String status = (String) card.get("status");
    if ("voided".equals(status)) {
      throw ApiException.badRequest("CARD_STATUS_INVALID", "已作废次卡不能调整");
    }
    int total = ((Number) card.get("total_times")).intValue();
    int remaining = ((Number) card.get("remaining_times")).intValue();
    int nextTotal = delta > 0 ? total + delta : total;
    int nextRemaining = remaining + delta;
    if (nextRemaining < 0 || nextRemaining > nextTotal) {
      throw ApiException.badRequest("CARD_TIMES_INVALID", "次卡剩余次数不合法");
    }
    String nextStatus = nextRemaining == 0 ? "used_up" : "valid";
    jdbcTemplate.update("""
        UPDATE member_cards
        SET total_times = ?, remaining_times = ?, status = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, nextTotal, nextRemaining, nextStatus, cardId);
    String reason = blankToNull(request.reason());
    long userId = ((Number) card.get("user_id")).longValue();
    insertCardTransaction(cardId, userId, "admin_adjust", delta, nextRemaining, "admin", reason);
    writeAuditLog(adminTokenDigest, "CARD_ADJUST", "card", cardId,
        "{\"deltaTimes\":%d,\"reason\":%s}".formatted(delta, jsonValue(reason)));
    return cardById(cardId);
  }

  @Transactional
  public MemberCardDto voidCard(String adminTokenDigest, long cardId, String reason) {
    Map<String, Object> card = requireCardForUpdate(cardId);
    String status = (String) card.get("status");
    if ("voided".equals(status)) {
      throw ApiException.badRequest("CARD_STATUS_INVALID", "次卡已经作废");
    }
    int remaining = ((Number) card.get("remaining_times")).intValue();
    jdbcTemplate.update("""
        UPDATE member_cards
        SET remaining_times = 0, status = 'voided', reason = COALESCE(?, reason), updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, blankToNull(reason), cardId);
    if (remaining > 0) {
      insertCardTransaction(cardId, ((Number) card.get("user_id")).longValue(), "void", -remaining, 0, "admin", blankToNull(reason));
    }
    writeAuditLog(adminTokenDigest, "CARD_VOID", "card", cardId,
        "{\"reason\":%s}".formatted(jsonValue(reason)));
    return cardById(cardId);
  }

  @Transactional
  public void grantPaidOrderPoints(long orderId, long userId, int amountCent, String source) {
    int points = amountCent / 100;
    if (points <= 0 || pointOrderTransactionExists(orderId, "order_paid")) {
      return;
    }
    adjustPointBalance(userId, points, "order_paid", source, orderId, "订单支付发放积分");
  }

  @Transactional
  public void reversePaidOrderPoints(long orderId, long userId, int amountCent, String source) {
    int points = amountCent / 100;
    if (points <= 0 || !pointOrderTransactionExists(orderId, "order_paid")
        || pointOrderTransactionExists(orderId, "order_refunded")) {
      return;
    }
    adjustPointBalance(userId, -points, "order_refunded", source, orderId, "订单退款回收积分");
  }

  private long adjustPointBalance(
      long userId,
      int delta,
      String type,
      String source,
      Long sourceOrderId,
      String reason) {
    ensurePointAccount(userId);
    Map<String, Object> account = jdbcTemplate.queryForList(
        "SELECT balance FROM member_point_accounts WHERE user_id = ? FOR UPDATE",
        userId).get(0);
    int balance = ((Number) account.get("balance")).intValue();
    int next = balance + delta;
    if (next < 0) {
      throw ApiException.badRequest("POINT_BALANCE_INVALID", "积分余额不足");
    }
    jdbcTemplate.update(
        "UPDATE member_point_accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
        next,
        userId);
    return insertPointTransaction(userId, type, delta, next, source, sourceOrderId, reason);
  }

  private long adjustWalletBalance(
      long userId,
      int delta,
      String type,
      String source,
      Long sourceOrderId,
      String reason) {
    ensureWalletAccount(userId);
    Map<String, Object> account = jdbcTemplate.queryForList(
        "SELECT balance_cent FROM member_wallet_accounts WHERE user_id = ? FOR UPDATE",
        userId).get(0);
    int balance = ((Number) account.get("balance_cent")).intValue();
    int next = balance + delta;
    if (next < 0) {
      throw ApiException.badRequest("WALLET_BALANCE_INVALID", "余额不足");
    }
    jdbcTemplate.update(
        "UPDATE member_wallet_accounts SET balance_cent = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
        next,
        userId);
    return insertWalletTransaction(userId, type, delta, next, source, sourceOrderId, reason);
  }

  private long insertPointTransaction(
      long userId,
      String type,
      int delta,
      int balanceAfter,
      String source,
      Long sourceOrderId,
      String reason) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO member_point_transactions(user_id, type, delta_points, balance_after, source, source_order_id, reason)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setString(2, type);
      ps.setInt(3, delta);
      ps.setInt(4, balanceAfter);
      ps.setString(5, source);
      if (sourceOrderId == null) {
        ps.setObject(6, null);
      } else {
        ps.setLong(6, sourceOrderId);
      }
      ps.setString(7, reason);
      return ps;
    }, keyHolder);
    return generatedId(keyHolder);
  }

  private long insertWalletTransaction(
      long userId,
      String type,
      int delta,
      int balanceAfter,
      String source,
      Long sourceOrderId,
      String reason) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO member_wallet_transactions(user_id, type, delta_cent, balance_after_cent, source, source_order_id, reason)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setString(2, type);
      ps.setInt(3, delta);
      ps.setInt(4, balanceAfter);
      ps.setString(5, source);
      if (sourceOrderId == null) {
        ps.setObject(6, null);
      } else {
        ps.setLong(6, sourceOrderId);
      }
      ps.setString(7, reason);
      return ps;
    }, keyHolder);
    return generatedId(keyHolder);
  }

  private void insertCardTransaction(
      long cardId,
      long userId,
      String type,
      int delta,
      int remainingAfter,
      String source,
      String reason) {
    jdbcTemplate.update("""
        INSERT INTO member_card_transactions(card_id, user_id, type, delta_times, remaining_after, source, reason)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """, cardId, userId, type, delta, remainingAfter, source, reason);
  }

  private void ensurePointAccount(long userId) {
    try {
      jdbcTemplate.update("INSERT INTO member_point_accounts(user_id, balance) VALUES (?, 0)", userId);
    } catch (DuplicateKeyException ignored) {
    }
  }

  private void ensureWalletAccount(long userId) {
    try {
      jdbcTemplate.update("INSERT INTO member_wallet_accounts(user_id, balance_cent) VALUES (?, 0)", userId);
    } catch (DuplicateKeyException ignored) {
    }
  }

  private boolean pointOrderTransactionExists(long orderId, String type) {
    return count(
        "SELECT COUNT(*) FROM member_point_transactions WHERE source_order_id = ? AND type = ?",
        orderId,
        type) > 0;
  }

  private MemberCouponDto couponById(long couponId) {
    List<MemberCouponDto> rows = jdbcTemplate.query(
        couponSql() + " WHERE c.id = ?",
        couponMapper(),
        couponId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("优惠券不存在");
    }
    return rows.get(0);
  }

  private MemberCardDto cardById(long cardId) {
    List<MemberCardDto> rows = jdbcTemplate.query(
        cardSql() + " WHERE c.id = ?",
        cardMapper(),
        cardId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("次卡不存在");
    }
    return rows.get(0);
  }

  private PointTransactionDto pointTransactionById(long transactionId) {
    List<PointTransactionDto> rows = jdbcTemplate.query("""
        SELECT id, type, delta_points, balance_after, source, source_order_id, reason, created_at
        FROM member_point_transactions
        WHERE id = ?
        """, pointTransactionMapper(), transactionId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("积分流水不存在");
    }
    return rows.get(0);
  }

  private WalletTransactionDto walletTransactionById(long transactionId) {
    List<WalletTransactionDto> rows = jdbcTemplate.query("""
        SELECT id, type, delta_cent, balance_after_cent, source, source_order_id, reason, created_at
        FROM member_wallet_transactions
        WHERE id = ?
        """, walletTransactionMapper(), transactionId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("余额流水不存在");
    }
    return rows.get(0);
  }

  private Map<String, Object> requireCouponForUpdate(long couponId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT id, user_id, status FROM member_coupons WHERE id = ? FOR UPDATE",
        couponId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("优惠券不存在");
    }
    return rows.get(0);
  }

  private Map<String, Object> requireCardForUpdate(long cardId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, user_id, total_times, remaining_times, status
        FROM member_cards
        WHERE id = ?
        FOR UPDATE
        """, cardId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("次卡不存在");
    }
    return rows.get(0);
  }

  private void requireUser(long userId) {
    requireExists("SELECT COUNT(*) FROM users WHERE id = ?", userId, "用户不存在");
  }

  private void requireExists(String sql, Object id, String message) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
    if (count == null || count == 0) {
      throw ApiException.notFound(message);
    }
  }

  private String couponSql() {
    return """
        SELECT c.id, c.title, c.coupon_type, c.amount_cent, c.threshold_cent,
               CASE WHEN c.status = 'available' AND c.expires_at < CURRENT_TIMESTAMP THEN 'expired'
                    ELSE c.status END AS effective_status,
               c.valid_from, c.expires_at, c.source, c.created_at
        FROM member_coupons c
        """;
  }

  private String cardSql() {
    return """
        SELECT c.id, c.title, c.applicable_service_id, s.name AS applicable_service_name,
               c.total_times, c.remaining_times,
               CASE WHEN c.status = 'valid' AND c.expires_at IS NOT NULL AND c.expires_at < CURRENT_TIMESTAMP THEN 'expired'
                    ELSE c.status END AS effective_status,
               c.valid_from, c.expires_at, c.source, c.created_at
        FROM member_cards c
        LEFT JOIN services s ON s.id = c.applicable_service_id
        """;
  }

  private RowMapper<MemberCouponDto> couponMapper() {
    return (rs, rowNum) -> new MemberCouponDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("title"),
        rs.getString("coupon_type"),
        rs.getInt("amount_cent"),
        rs.getInt("threshold_cent"),
        rs.getString("effective_status"),
        timestampToUtc(rs, "valid_from"),
        timestampToUtc(rs, "expires_at"),
        rs.getString("source"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<MemberCardDto> cardMapper() {
    return (rs, rowNum) -> new MemberCardDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("title"),
        nullableId(rs, "applicable_service_id"),
        rs.getString("applicable_service_name"),
        rs.getInt("total_times"),
        rs.getInt("remaining_times"),
        rs.getString("effective_status"),
        timestampToUtc(rs, "valid_from"),
        timestampToUtc(rs, "expires_at"),
        rs.getString("source"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<PointTransactionDto> pointTransactionMapper() {
    return (rs, rowNum) -> new PointTransactionDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("type"),
        rs.getInt("delta_points"),
        rs.getInt("balance_after"),
        rs.getString("source"),
        nullableId(rs, "source_order_id"),
        rs.getString("reason"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<WalletTransactionDto> walletTransactionMapper() {
    return (rs, rowNum) -> new WalletTransactionDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("type"),
        rs.getInt("delta_cent"),
        rs.getInt("balance_after_cent"),
        rs.getString("source"),
        nullableId(rs, "source_order_id"),
        rs.getString("reason"),
        timestampToUtc(rs, "created_at"));
  }

  private int intValue(String sql, Object... params) {
    List<Integer> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt(1), params);
    return rows.isEmpty() ? 0 : rows.get(0);
  }

  private int count(String sql, Object... params) {
    Integer value = jdbcTemplate.queryForObject(sql, Integer.class, params);
    return value == null ? 0 : value;
  }

  private int normalizeLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  private String normalizeCouponFilter(String status) {
    if (status == null || status.isBlank()) {
      return "available";
    }
    String normalized = status.trim();
    if (!"available".equals(normalized) && !"used".equals(normalized) && !"expired".equals(normalized)
        && !"voided".equals(normalized) && !"all".equals(normalized)) {
      throw ApiException.badRequest("COUPON_STATUS_INVALID", "优惠券状态不合法");
    }
    return normalized;
  }

  private String normalizeCardFilter(String status) {
    if (status == null || status.isBlank()) {
      return "valid";
    }
    String normalized = status.trim();
    if (!"valid".equals(normalized) && !"used_up".equals(normalized) && !"expired".equals(normalized)
        && !"voided".equals(normalized) && !"all".equals(normalized)) {
      throw ApiException.badRequest("CARD_STATUS_INVALID", "次卡状态不合法");
    }
    return normalized;
  }

  private String normalizeCouponType(String couponType) {
    if (couponType == null || couponType.isBlank()) {
      return "amount";
    }
    if (!"amount".equals(couponType)) {
      throw ApiException.badRequest("COUPON_TYPE_INVALID", "优惠券类型暂只支持 amount");
    }
    return couponType;
  }

  private int requirePositive(Integer value, String message) {
    if (value == null || value <= 0) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return value;
  }

  private int requireDelta(Integer value, String message) {
    if (value == null || value == 0) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return value;
  }

  private String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return value.trim();
  }

  private Timestamp requireTimestamp(String value, String message) {
    Timestamp timestamp = optionalTimestamp(value);
    if (timestamp == null) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return timestamp;
  }

  private Timestamp optionalTimestamp(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Timestamp.valueOf(LocalDateTime.parse(value.trim()));
    } catch (DateTimeParseException ex) {
      throw ApiException.badRequest("VALIDATION_ERROR", "时间格式必须为 yyyy-MM-ddTHH:mm:ss");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String jsonValue(String value) {
    if (value == null || value.isBlank()) {
      return "null";
    }
    return "\"%s\"".formatted(value.replace("\\", "\\\\").replace("\"", "\\\""));
  }

  private String nullableId(ResultSet rs, String column) throws SQLException {
    Object value = rs.getObject(column);
    return value == null ? null : String.valueOf(rs.getLong(column));
  }

  private String timestampToUtc(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
  }

  private long generatedId(KeyHolder keyHolder) {
    List<Map<String, Object>> keys = keyHolder.getKeyList();
    if (keys.isEmpty()) {
      throw new IllegalStateException("Generated key is missing");
    }
    Object id = keys.get(0).get("id");
    if (id == null && keys.get(0).size() == 1) {
      id = keys.get(0).values().iterator().next();
    }
    if (!(id instanceof Number number)) {
      throw new IllegalStateException("Generated id is missing");
    }
    return number.longValue();
  }

  private void writeAuditLog(
      String adminTokenDigest,
      String action,
      String targetType,
      long targetId,
      String detailJson) {
    jdbcTemplate.update("""
        INSERT INTO admin_operation_logs(admin_token_digest, action, target_type, target_id, detail_json)
        VALUES (?, ?, ?, ?, ?)
        """, adminTokenDigest, action, targetType, targetId, detailJson);
  }
}
