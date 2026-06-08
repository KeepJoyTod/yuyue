package com.amberfilm.admin;

import com.amberfilm.admin.AdminDtos.AdminNegativeDto;
import com.amberfilm.admin.AdminDtos.AdminOrderDto;
import com.amberfilm.admin.AdminDtos.AdminScheduleDto;
import com.amberfilm.admin.AdminDtos.AdminServiceDto;
import com.amberfilm.admin.AdminDtos.AdminStoreDto;
import com.amberfilm.admin.AdminDtos.AdminSummaryDto;
import com.amberfilm.admin.AdminDtos.AdminAuditLogDto;
import com.amberfilm.admin.AdminDtos.UpdateOrderRequest;
import com.amberfilm.admin.AdminDtos.UpsertScheduleRequest;
import com.amberfilm.admin.AdminDtos.UpsertServiceRequest;
import com.amberfilm.admin.AdminDtos.UpsertStoreRequest;
import com.amberfilm.common.ApiException;
import com.amberfilm.file.FileService;
import com.amberfilm.member.MemberAssetService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final JdbcTemplate jdbcTemplate;
  private final FileService fileService;
  private final MemberAssetService memberAssetService;

  public AdminService(JdbcTemplate jdbcTemplate, FileService fileService, MemberAssetService memberAssetService) {
    this.jdbcTemplate = jdbcTemplate;
    this.fileService = fileService;
    this.memberAssetService = memberAssetService;
  }

  public AdminSummaryDto summary() {
    return new AdminSummaryDto(
        count("SELECT COUNT(*) FROM users"),
        count("SELECT COUNT(*) FROM services"),
        count("SELECT COUNT(*) FROM stores"),
        count("SELECT COUNT(*) FROM schedules"),
        count("SELECT COUNT(*) FROM orders"),
        count("SELECT COUNT(*) FROM orders WHERE status = 'pending'"),
        count("SELECT COUNT(*) FROM orders WHERE status = 'confirmed'"),
        count("SELECT COUNT(*) FROM orders WHERE status = 'completed'"),
        count("SELECT COUNT(*) FROM negatives"),
        count("SELECT COUNT(*) FROM negatives WHERE status = 'visible'"),
        count("SELECT COALESCE(SUM(price_cent), 0) FROM orders WHERE pay_status = 'paid'"));
  }

  public List<AdminServiceDto> services() {
    return jdbcTemplate.query("""
        SELECT s.id, s.category_id, c.name AS category_name, s.name, s.cover_url,
               s.price_cent, s.duration_min, s.description, s.tags_json, s.rating,
               s.enabled, s.created_at
        FROM services s
        JOIN service_categories c ON c.id = s.category_id
        ORDER BY c.sort_order ASC, s.id ASC
        """, serviceMapper());
  }

  @Transactional
  public AdminServiceDto createService(String adminTokenDigest, UpsertServiceRequest request) {
    requireCategory(request.categoryId());
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO services(category_id, name, cover_url, price_cent, duration_min, description, tags_json, rating, enabled)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, request.categoryId());
      ps.setString(2, requireText(request.name(), "套餐名称不能为空"));
      ps.setString(3, request.coverUrl());
      ps.setInt(4, requirePositive(request.priceCent(), "套餐价格必须大于 0"));
      ps.setInt(5, requirePositive(request.durationMin(), "服务时长必须大于 0"));
      ps.setString(6, request.description());
      ps.setString(7, request.tagsJson());
      ps.setBigDecimal(8, request.rating());
      ps.setBoolean(9, request.enabled() == null || request.enabled());
      return ps;
    }, keyHolder);
    long id = generatedId(keyHolder);
    writeAuditLog(adminTokenDigest, "SERVICE_CREATE", "service", id, "{\"id\":%d}".formatted(id));
    return serviceById(id);
  }

  @Transactional
  public AdminServiceDto updateService(String adminTokenDigest, long id, UpsertServiceRequest request) {
    requireExists("SELECT COUNT(*) FROM services WHERE id = ?", id, "套餐不存在");
    if (request.categoryId() != null) {
      requireCategory(request.categoryId());
    }
    jdbcTemplate.update("""
        UPDATE services
        SET category_id = COALESCE(?, category_id),
            name = COALESCE(?, name),
            cover_url = COALESCE(?, cover_url),
            price_cent = COALESCE(?, price_cent),
            duration_min = COALESCE(?, duration_min),
            description = COALESCE(?, description),
            tags_json = COALESCE(?, tags_json),
            rating = COALESCE(?, rating),
            enabled = COALESCE(?, enabled),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        request.categoryId(),
        blankToNull(request.name()),
        request.coverUrl(),
        request.priceCent(),
        request.durationMin(),
        request.description(),
        request.tagsJson(),
        request.rating(),
        request.enabled(),
        id);
    writeAuditLog(adminTokenDigest, "SERVICE_UPDATE", "service", id, "{\"id\":%d}".formatted(id));
    return serviceById(id);
  }

  public List<AdminStoreDto> stores() {
    return jdbcTemplate.query("""
        SELECT id, name, address, distance_km, rating, reviews, hours, tags_json,
               cover_url, enabled, created_at
        FROM stores
        ORDER BY id ASC
        """, storeMapper());
  }

  @Transactional
  public AdminStoreDto createStore(String adminTokenDigest, UpsertStoreRequest request) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO stores(name, address, distance_km, rating, reviews, hours, tags_json, cover_url, enabled)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, requireText(request.name(), "门店名称不能为空"));
      ps.setString(2, requireText(request.address(), "门店地址不能为空"));
      ps.setBigDecimal(3, request.distanceKm());
      ps.setBigDecimal(4, request.rating());
      ps.setInt(5, request.reviews() == null ? 0 : Math.max(0, request.reviews()));
      ps.setString(6, request.hours());
      ps.setString(7, request.tagsJson());
      ps.setString(8, request.coverUrl());
      ps.setBoolean(9, request.enabled() == null || request.enabled());
      return ps;
    }, keyHolder);
    long id = generatedId(keyHolder);
    writeAuditLog(adminTokenDigest, "STORE_CREATE", "store", id, "{\"id\":%d}".formatted(id));
    return storeById(id);
  }

  @Transactional
  public AdminStoreDto updateStore(String adminTokenDigest, long id, UpsertStoreRequest request) {
    requireExists("SELECT COUNT(*) FROM stores WHERE id = ?", id, "门店不存在");
    jdbcTemplate.update("""
        UPDATE stores
        SET name = COALESCE(?, name),
            address = COALESCE(?, address),
            distance_km = COALESCE(?, distance_km),
            rating = COALESCE(?, rating),
            reviews = COALESCE(?, reviews),
            hours = COALESCE(?, hours),
            tags_json = COALESCE(?, tags_json),
            cover_url = COALESCE(?, cover_url),
            enabled = COALESCE(?, enabled),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        blankToNull(request.name()),
        blankToNull(request.address()),
        request.distanceKm(),
        request.rating(),
        request.reviews(),
        request.hours(),
        request.tagsJson(),
        request.coverUrl(),
        request.enabled(),
        id);
    writeAuditLog(adminTokenDigest, "STORE_UPDATE", "store", id, "{\"id\":%d}".formatted(id));
    return storeById(id);
  }

  public List<AdminScheduleDto> schedules(LocalDate date) {
    if (date == null) {
      return jdbcTemplate.query(scheduleSql() + " ORDER BY sc.service_date ASC, sc.start_time ASC", scheduleMapper());
    }
    return jdbcTemplate.query(
        scheduleSql() + " WHERE sc.service_date = ? ORDER BY sc.start_time ASC",
        scheduleMapper(),
        date);
  }

  @Transactional
  public AdminScheduleDto createSchedule(String adminTokenDigest, UpsertScheduleRequest request) {
    requireExists("SELECT COUNT(*) FROM stores WHERE id = ?", request.storeId(), "门店不存在");
    if (request.serviceId() != null) {
      requireExists("SELECT COUNT(*) FROM services WHERE id = ?", request.serviceId(), "套餐不存在");
    }
    LocalDate serviceDate = LocalDate.parse(requireText(request.serviceDate(), "档期日期不能为空"));
    LocalTime startTime = LocalTime.parse(requireText(request.startTime(), "开始时间不能为空"));
    LocalTime endTime = LocalTime.parse(requireText(request.endTime(), "结束时间不能为空"));
    if (!endTime.isAfter(startTime)) {
      throw ApiException.badRequest("SCHEDULE_TIME_INVALID", "结束时间必须晚于开始时间");
    }
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO schedules(store_id, service_id, service_date, start_time, end_time, capacity, booked_count, status)
          VALUES (?, ?, ?, ?, ?, ?, 0, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, request.storeId());
      ps.setObject(2, request.serviceId());
      ps.setObject(3, serviceDate);
      ps.setObject(4, startTime);
      ps.setObject(5, endTime);
      ps.setInt(6, requirePositive(request.capacity(), "档期容量必须大于 0"));
      ps.setString(7, normalizeScheduleStatus(request.status()));
      return ps;
    }, keyHolder);
    long id = generatedId(keyHolder);
    writeAuditLog(adminTokenDigest, "SCHEDULE_CREATE", "schedule", id, "{\"id\":%d}".formatted(id));
    return scheduleById(id);
  }

  @Transactional
  public AdminScheduleDto updateSchedule(String adminTokenDigest, long id, UpsertScheduleRequest request) {
    Map<String, Object> schedule = requireSchedule(id);
    if (request.storeId() != null) {
      requireExists("SELECT COUNT(*) FROM stores WHERE id = ?", request.storeId(), "门店不存在");
    }
    if (request.serviceId() != null) {
      requireExists("SELECT COUNT(*) FROM services WHERE id = ?", request.serviceId(), "套餐不存在");
    }
    Integer capacity = request.capacity();
    if (capacity != null && capacity < ((Number) schedule.get("booked_count")).intValue()) {
      throw ApiException.badRequest("SCHEDULE_CAPACITY_INVALID", "档期容量不能小于已预约数量");
    }
    jdbcTemplate.update("""
        UPDATE schedules
        SET store_id = COALESCE(?, store_id),
            service_id = COALESCE(?, service_id),
            service_date = COALESCE(?, service_date),
            start_time = COALESCE(?, start_time),
            end_time = COALESCE(?, end_time),
            capacity = COALESCE(?, capacity),
            status = COALESCE(?, status),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        request.storeId(),
        request.serviceId(),
        request.serviceDate() == null ? null : LocalDate.parse(request.serviceDate()),
        request.startTime() == null ? null : LocalTime.parse(request.startTime()),
        request.endTime() == null ? null : LocalTime.parse(request.endTime()),
        capacity,
        request.status() == null ? null : normalizeScheduleStatus(request.status()),
        id);
    writeAuditLog(adminTokenDigest, "SCHEDULE_UPDATE", "schedule", id, "{\"id\":%d}".formatted(id));
    return scheduleById(id);
  }

  public List<AdminOrderDto> orders(String status) {
    if (status == null || status.isBlank() || "all".equals(status)) {
      return jdbcTemplate.query(orderSql() + " ORDER BY o.created_at DESC", orderMapper());
    }
    return jdbcTemplate.query(
        orderSql() + " WHERE o.status = ? ORDER BY o.created_at DESC",
        orderMapper(),
        status);
  }

  @Transactional
  public AdminOrderDto updateOrder(String adminTokenDigest, long id, UpdateOrderRequest request) {
    Map<String, Object> order = requireOrderForUpdate(id);
    String oldPayStatus = (String) order.get("pay_status");
    String status = request.status() == null ? null : normalizeOrderStatus(request.status());
    String payStatus = request.payStatus() == null ? null : normalizePayStatus(request.payStatus());
    jdbcTemplate.update("""
        UPDATE orders
        SET status = COALESCE(?, status),
            pay_status = COALESCE(?, pay_status),
            user_hidden = COALESCE(?, user_hidden),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, status, payStatus, request.userHidden(), id);
    writeAuditLog(
        adminTokenDigest,
        "ORDER_UPDATE",
        "order",
        id,
        "{\"id\":%d,\"status\":%s,\"payStatus\":%s}"
            .formatted(id, jsonValue(status), jsonValue(payStatus)));
    if (payStatus != null && !payStatus.equals(oldPayStatus)) {
      long userId = ((Number) order.get("user_id")).longValue();
      int priceCent = ((Number) order.get("price_cent")).intValue();
      if ("paid".equals(payStatus)) {
        memberAssetService.grantPaidOrderPoints(id, userId, priceCent, "admin_order");
      } else if ("refunded".equals(payStatus) && "paid".equals(oldPayStatus)) {
        memberAssetService.reversePaidOrderPoints(id, userId, priceCent, "admin_order");
      }
    }
    return orderById(id);
  }

  public List<AdminNegativeDto> negatives(Long userId, Long orderId) {
    StringBuilder sql = new StringBuilder("""
        SELECT n.id, n.user_id, u.phone AS user_phone, n.order_id, o.order_no,
               n.title, n.type, n.image_url, n.file_id, f.object_key, n.status, n.created_at
        FROM negatives n
        JOIN users u ON u.id = n.user_id
        JOIN orders o ON o.id = n.order_id
        LEFT JOIN files f ON f.id = n.file_id
        WHERE 1 = 1
        """);
    List<Object> params = new ArrayList<>();
    if (userId != null) {
      sql.append(" AND n.user_id = ?");
      params.add(userId);
    }
    if (orderId != null) {
      sql.append(" AND n.order_id = ?");
      params.add(orderId);
    }
    sql.append(" ORDER BY n.created_at DESC");
    return jdbcTemplate.query(sql.toString(), negativeMapper(), params.toArray());
  }

  @Transactional
  public AdminNegativeDto createNegative(String adminTokenDigest, CreateAdminNegativeRequest request) {
    Map<String, Object> order = requireOrder(request.orderId());
    long userId = ((Number) order.get("user_id")).longValue();
    String status = normalizeNegativeStatus(request.status());
    String imageUrl = normalizeNegativeImageUrl(request);
    Long fileId = request.fileId();
    if (fileId != null) {
      requireOwnedFile(fileId, userId);
      imageUrl = "file://" + fileId;
    }
    String finalImageUrl = imageUrl;
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO negatives(user_id, order_id, title, type, image_url, file_id, status)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setLong(2, request.orderId());
      ps.setString(3, request.title());
      ps.setString(4, request.type());
      ps.setString(5, finalImageUrl);
      if (fileId == null) {
        ps.setObject(6, null);
      } else {
        ps.setLong(6, fileId);
      }
      ps.setString(7, status);
      return ps;
    }, keyHolder);
    long negativeId = generatedId(keyHolder);
    writeAuditLog(
        adminTokenDigest,
        "NEGATIVE_CREATE",
        "negative",
        negativeId,
        "{\"orderId\":%d,\"userId\":%d,\"fileId\":%s,\"type\":\"%s\",\"status\":\"%s\"}"
            .formatted(request.orderId(), userId, fileId == null ? "null" : fileId, request.type(), status));
    return negativeById(negativeId);
  }

  public List<AdminAuditLogDto> auditLogs(String action, String targetType, Long targetId) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, action, target_type, target_id, detail_json, created_at
        FROM admin_operation_logs
        WHERE 1 = 1
        """);
    List<Object> params = new ArrayList<>();
    if (action != null && !action.isBlank()) {
      sql.append(" AND action = ?");
      params.add(action);
    }
    if (targetType != null && !targetType.isBlank()) {
      sql.append(" AND target_type = ?");
      params.add(targetType);
    }
    if (targetId != null) {
      sql.append(" AND target_id = ?");
      params.add(targetId);
    }
    sql.append(" ORDER BY created_at DESC");
    return jdbcTemplate.query(sql.toString(), auditLogMapper(), params.toArray());
  }

  private int count(String sql) {
    Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private AdminServiceDto serviceById(long id) {
    List<AdminServiceDto> rows = jdbcTemplate.query("""
        SELECT s.id, s.category_id, c.name AS category_name, s.name, s.cover_url,
               s.price_cent, s.duration_min, s.description, s.tags_json, s.rating,
               s.enabled, s.created_at
        FROM services s
        JOIN service_categories c ON c.id = s.category_id
        WHERE s.id = ?
        """, serviceMapper(), id);
    if (rows.isEmpty()) {
      throw ApiException.notFound("套餐不存在");
    }
    return rows.get(0);
  }

  private AdminStoreDto storeById(long id) {
    List<AdminStoreDto> rows = jdbcTemplate.query("""
        SELECT id, name, address, distance_km, rating, reviews, hours, tags_json,
               cover_url, enabled, created_at
        FROM stores
        WHERE id = ?
        """, storeMapper(), id);
    if (rows.isEmpty()) {
      throw ApiException.notFound("门店不存在");
    }
    return rows.get(0);
  }

  private AdminScheduleDto scheduleById(long id) {
    List<AdminScheduleDto> rows = jdbcTemplate.query(scheduleSql() + " WHERE sc.id = ?", scheduleMapper(), id);
    if (rows.isEmpty()) {
      throw ApiException.notFound("档期不存在");
    }
    return rows.get(0);
  }

  private AdminOrderDto orderById(long id) {
    List<AdminOrderDto> rows = jdbcTemplate.query(orderSql() + " WHERE o.id = ?", orderMapper(), id);
    if (rows.isEmpty()) {
      throw ApiException.notFound("订单不存在");
    }
    return rows.get(0);
  }

  private void requireCategory(Long categoryId) {
    if (categoryId == null) {
      throw ApiException.badRequest("CATEGORY_REQUIRED", "套餐分类不能为空");
    }
    requireExists("SELECT COUNT(*) FROM service_categories WHERE id = ?", categoryId, "套餐分类不存在");
  }

  private void requireExists(String sql, Object id, String message) {
    if (id == null) {
      throw ApiException.badRequest("ID_REQUIRED", message);
    }
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
    if (count == null || count == 0) {
      throw ApiException.notFound(message);
    }
  }

  private Map<String, Object> requireSchedule(long scheduleId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT id, booked_count FROM schedules WHERE id = ? FOR UPDATE",
        scheduleId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("档期不存在");
    }
    return rows.get(0);
  }

  private String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return value.trim();
  }

  private Integer requirePositive(Integer value, String message) {
    if (value == null || value <= 0) {
      throw ApiException.badRequest("VALIDATION_ERROR", message);
    }
    return value;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeScheduleStatus(String status) {
    if (status == null || status.isBlank()) {
      return "available";
    }
    if (!"available".equals(status) && !"closed".equals(status) && !"full".equals(status)) {
      throw ApiException.badRequest("SCHEDULE_STATUS_INVALID", "档期状态必须是 available、closed 或 full");
    }
    return status;
  }

  private String normalizeOrderStatus(String status) {
    if (!"pending".equals(status) && !"confirmed".equals(status) && !"completed".equals(status)
        && !"cancelled".equals(status) && !"refunded".equals(status)) {
      throw ApiException.badRequest("ORDER_STATUS_INVALID", "订单状态不合法");
    }
    return status;
  }

  private String normalizePayStatus(String status) {
    if (!"unpaid".equals(status) && !"paid".equals(status) && !"refunded".equals(status)) {
      throw ApiException.badRequest("PAY_STATUS_INVALID", "支付状态不合法");
    }
    return status;
  }

  private String jsonValue(String value) {
    return value == null ? "null" : "\"%s\"".formatted(value);
  }

  private String scheduleSql() {
    return """
        SELECT sc.id, sc.store_id, st.name AS store_name, sc.service_id, sv.name AS service_name,
               sc.service_date, sc.start_time, sc.end_time, sc.capacity, sc.booked_count,
               sc.status, sc.created_at
        FROM schedules sc
        JOIN stores st ON st.id = sc.store_id
        LEFT JOIN services sv ON sv.id = sc.service_id
        """;
  }

  private String orderSql() {
    return """
        SELECT o.id, o.order_no, o.user_id, u.phone AS user_phone, o.service_id, s.name AS service_name,
               o.store_id, st.name AS store_name, o.schedule_id, o.contact_name, o.contact_phone,
               o.price_cent, o.appointment_at, o.status, o.pay_status, o.user_hidden, o.created_at
        FROM orders o
        JOIN users u ON u.id = o.user_id
        JOIN services s ON s.id = o.service_id
        JOIN stores st ON st.id = o.store_id
        """;
  }

  private Map<String, Object> requireOrder(long orderId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT id, user_id FROM orders WHERE id = ?",
        orderId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("订单不存在，无法绑定底片");
    }
    return rows.get(0);
  }

  private Map<String, Object> requireOrderForUpdate(long orderId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, user_id, price_cent, pay_status
        FROM orders
        WHERE id = ?
        FOR UPDATE
        """, orderId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("订单不存在");
    }
    return rows.get(0);
  }

  private String normalizeNegativeStatus(String status) {
    if (status == null || status.isBlank()) {
      return "visible";
    }
    if (!"visible".equals(status) && !"hidden".equals(status)) {
      throw ApiException.badRequest("NEGATIVE_STATUS_INVALID", "底片状态必须是 visible 或 hidden");
    }
    return status;
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

  private AdminNegativeDto negativeById(long negativeId) {
    List<AdminNegativeDto> negatives = jdbcTemplate.query("""
        SELECT n.id, n.user_id, u.phone AS user_phone, n.order_id, o.order_no,
               n.title, n.type, n.image_url, n.file_id, f.object_key, n.status, n.created_at
        FROM negatives n
        JOIN users u ON u.id = n.user_id
        JOIN orders o ON o.id = n.order_id
        LEFT JOIN files f ON f.id = n.file_id
        WHERE n.id = ?
        """, negativeMapper(), negativeId);
    if (negatives.isEmpty()) {
      throw ApiException.notFound("底片不存在");
    }
    return negatives.get(0);
  }

  private RowMapper<AdminServiceDto> serviceMapper() {
    return (rs, rowNum) -> new AdminServiceDto(
        String.valueOf(rs.getLong("id")),
        String.valueOf(rs.getLong("category_id")),
        rs.getString("category_name"),
        rs.getString("name"),
        rs.getString("cover_url"),
        rs.getInt("price_cent"),
        rs.getInt("duration_min"),
        rs.getString("description"),
        rs.getString("tags_json"),
        rs.getBigDecimal("rating"),
        rs.getBoolean("enabled"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<AdminStoreDto> storeMapper() {
    return (rs, rowNum) -> new AdminStoreDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("name"),
        rs.getString("address"),
        rs.getBigDecimal("distance_km"),
        rs.getBigDecimal("rating"),
        rs.getInt("reviews"),
        rs.getString("hours"),
        rs.getString("tags_json"),
        rs.getString("cover_url"),
        rs.getBoolean("enabled"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<AdminScheduleDto> scheduleMapper() {
    return (rs, rowNum) -> new AdminScheduleDto(
        String.valueOf(rs.getLong("id")),
        String.valueOf(rs.getLong("store_id")),
        rs.getString("store_name"),
        nullableId(rs, "service_id"),
        rs.getString("service_name"),
        rs.getDate("service_date").toLocalDate().toString(),
        rs.getTime("start_time").toLocalTime().toString(),
        rs.getTime("end_time").toLocalTime().toString(),
        rs.getInt("capacity"),
        rs.getInt("booked_count"),
        rs.getString("status"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<AdminOrderDto> orderMapper() {
    return (rs, rowNum) -> new AdminOrderDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("order_no"),
        String.valueOf(rs.getLong("user_id")),
        rs.getString("user_phone"),
        String.valueOf(rs.getLong("service_id")),
        rs.getString("service_name"),
        String.valueOf(rs.getLong("store_id")),
        rs.getString("store_name"),
        String.valueOf(rs.getLong("schedule_id")),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getInt("price_cent"),
        rs.getTimestamp("appointment_at").toLocalDateTime().toString(),
        rs.getString("status"),
        rs.getString("pay_status"),
        rs.getBoolean("user_hidden"),
        timestampToUtc(rs, "created_at"));
  }

  private RowMapper<AdminNegativeDto> negativeMapper() {
    return (rs, rowNum) -> new AdminNegativeDto(
        String.valueOf(rs.getLong("id")),
        String.valueOf(rs.getLong("user_id")),
        rs.getString("user_phone"),
        String.valueOf(rs.getLong("order_id")),
        rs.getString("order_no"),
        rs.getString("title"),
        rs.getString("type"),
        rs.getString("image_url"),
        nullableId(rs, "file_id"),
        rs.getString("object_key") == null ? rs.getString("image_url") : fileService.signedDownloadUrl(rs.getString("object_key")),
        rs.getString("status"),
        timestampToUtc(rs, "created_at"));
  }

  private void requireOwnedFile(long fileId, long userId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM files WHERE id = ? AND owner_user_id = ?",
        Integer.class,
        fileId,
        userId);
    if (count == null || count == 0) {
      throw ApiException.badRequest("FILE_OWNER_MISMATCH", "文件不存在或不属于该订单用户");
    }
  }

  private String normalizeNegativeImageUrl(CreateAdminNegativeRequest request) {
    if (request.fileId() != null) {
      return "file://" + request.fileId();
    }
    if (request.imageUrl() == null || request.imageUrl().isBlank()) {
      throw ApiException.badRequest("NEGATIVE_FILE_REQUIRED", "必须提供 fileId 或 imageUrl");
    }
    return request.imageUrl();
  }

  private RowMapper<AdminAuditLogDto> auditLogMapper() {
    return (rs, rowNum) -> new AdminAuditLogDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("action"),
        rs.getString("target_type"),
        nullableId(rs, "target_id"),
        rs.getString("detail_json"),
        timestampToUtc(rs, "created_at"));
  }

  private String nullableId(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : String.valueOf(value);
  }

  private String timestampToUtc(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column).toInstant().atOffset(ZoneOffset.UTC).toString();
  }
}
