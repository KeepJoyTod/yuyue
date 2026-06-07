package com.amberfilm.admin;

import com.amberfilm.admin.AdminDtos.AdminNegativeDto;
import com.amberfilm.admin.AdminDtos.AdminOrderDto;
import com.amberfilm.admin.AdminDtos.AdminScheduleDto;
import com.amberfilm.admin.AdminDtos.AdminServiceDto;
import com.amberfilm.admin.AdminDtos.AdminStoreDto;
import com.amberfilm.admin.AdminDtos.AdminSummaryDto;
import com.amberfilm.admin.AdminDtos.AdminAuditLogDto;
import com.amberfilm.common.ApiException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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

  public AdminService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

  public List<AdminStoreDto> stores() {
    return jdbcTemplate.query("""
        SELECT id, name, address, distance_km, rating, reviews, hours, tags_json,
               cover_url, enabled, created_at
        FROM stores
        ORDER BY id ASC
        """, storeMapper());
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

  public List<AdminOrderDto> orders(String status) {
    if (status == null || status.isBlank() || "all".equals(status)) {
      return jdbcTemplate.query(orderSql() + " ORDER BY o.created_at DESC", orderMapper());
    }
    return jdbcTemplate.query(
        orderSql() + " WHERE o.status = ? ORDER BY o.created_at DESC",
        orderMapper(),
        status);
  }

  public List<AdminNegativeDto> negatives(Long userId, Long orderId) {
    StringBuilder sql = new StringBuilder("""
        SELECT n.id, n.user_id, u.phone AS user_phone, n.order_id, o.order_no,
               n.title, n.type, n.image_url, n.status, n.created_at
        FROM negatives n
        JOIN users u ON u.id = n.user_id
        JOIN orders o ON o.id = n.order_id
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
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO negatives(user_id, order_id, title, type, image_url, status)
          VALUES (?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setLong(2, request.orderId());
      ps.setString(3, request.title());
      ps.setString(4, request.type());
      ps.setString(5, request.imageUrl());
      ps.setString(6, status);
      return ps;
    }, keyHolder);
    long negativeId = generatedId(keyHolder);
    writeAuditLog(
        adminTokenDigest,
        "NEGATIVE_CREATE",
        "negative",
        negativeId,
        "{\"orderId\":%d,\"userId\":%d,\"type\":\"%s\",\"status\":\"%s\"}"
            .formatted(request.orderId(), userId, request.type(), status));
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
               n.title, n.type, n.image_url, n.status, n.created_at
        FROM negatives n
        JOIN users u ON u.id = n.user_id
        JOIN orders o ON o.id = n.order_id
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
        rs.getString("status"),
        timestampToUtc(rs, "created_at"));
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
