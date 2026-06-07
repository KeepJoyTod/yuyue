package com.amberfilm.order;

import com.amberfilm.common.ApiException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final JdbcTemplate jdbcTemplate;

  public OrderService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<OrderDto> list(long userId, String status) {
    if (status == null || status.isBlank() || "all".equals(status)) {
      return jdbcTemplate.query(baseSql() + " AND o.user_hidden = FALSE ORDER BY o.created_at DESC", mapper(), userId);
    }
    return jdbcTemplate.query(
        baseSql() + " AND o.user_hidden = FALSE AND o.status = ? ORDER BY o.created_at DESC",
        mapper(),
        userId,
        status);
  }

  public OrderDto detail(long userId, long orderId) {
    List<OrderDto> orders = jdbcTemplate.query(
        baseSql() + " AND o.id = ?",
        mapper(),
        userId,
        orderId);
    if (orders.isEmpty()) {
      throw ApiException.notFound("订单不存在");
    }
    return orders.get(0);
  }

  @Transactional
  public OrderDto pay(long userId, long orderId) {
    Map<String, Object> order = requireMutableOrder(userId, orderId);
    if (!"pending".equals(order.get("status"))) {
      throw ApiException.badRequest("ORDER_STATUS_INVALID", "只有待支付订单可以支付");
    }
    jdbcTemplate.update("""
        UPDATE orders
        SET status = 'confirmed', pay_status = 'paid', updated_at = CURRENT_TIMESTAMP
        WHERE id = ? AND user_id = ?
        """, orderId, userId);
    jdbcTemplate.update("""
        INSERT INTO payments(order_id, channel, amount_cent, status, transaction_no, paid_at)
        VALUES (?, 'mock', ?, 'success', ?, CURRENT_TIMESTAMP)
        """, orderId, ((Number) order.get("price_cent")).intValue(), "MOCK" + order.get("order_no"));
    return detail(userId, orderId);
  }

  @Transactional
  public OrderDto cancel(long userId, long orderId) {
    Map<String, Object> order = requireMutableOrder(userId, orderId);
    String status = (String) order.get("status");
    if (!"pending".equals(status) && !"confirmed".equals(status)) {
      throw ApiException.badRequest("ORDER_STATUS_INVALID", "当前订单状态不可取消");
    }
    jdbcTemplate.update("""
        UPDATE orders
        SET status = 'cancelled',
            pay_status = CASE WHEN pay_status = 'paid' THEN 'refunded' ELSE pay_status END,
            cancelled_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ? AND user_id = ?
        """, orderId, userId);
    jdbcTemplate.update("""
        UPDATE schedules
        SET booked_count = CASE WHEN booked_count > 0 THEN booked_count - 1 ELSE 0 END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, ((Number) order.get("schedule_id")).longValue());
    return detail(userId, orderId);
  }

  @Transactional
  public OrderDto complete(long userId, long orderId) {
    Map<String, Object> order = requireMutableOrder(userId, orderId);
    if (!"confirmed".equals(order.get("status"))) {
      throw ApiException.badRequest("ORDER_STATUS_INVALID", "只有已预约订单可以完成");
    }
    jdbcTemplate.update("""
        UPDATE orders
        SET status = 'completed', updated_at = CURRENT_TIMESTAMP
        WHERE id = ? AND user_id = ?
        """, orderId, userId);
    return detail(userId, orderId);
  }

  @Transactional
  public void hide(long userId, long orderId) {
    int affected = jdbcTemplate.update(
        "UPDATE orders SET user_hidden = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
        orderId,
        userId);
    if (affected == 0) {
      throw ApiException.notFound("订单不存在");
    }
  }

  private Map<String, Object> requireMutableOrder(long userId, long orderId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, order_no, user_id, schedule_id, price_cent, status, pay_status
        FROM orders
        WHERE id = ? AND user_id = ?
        FOR UPDATE
        """, orderId, userId);
    if (rows.isEmpty()) {
      throw new ApiException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
    }
    return rows.get(0);
  }

  private String baseSql() {
    return """
        SELECT o.id, o.order_no, o.service_id, s.name AS service_name, s.cover_url AS service_cover_url,
               o.store_id, st.name AS store_name, st.address AS store_address,
               o.price_cent, s.duration_min, o.contact_name, o.contact_phone,
               o.appointment_at, o.status, o.pay_status, o.created_at
        FROM orders o
        JOIN services s ON s.id = o.service_id
        JOIN stores st ON st.id = o.store_id
        WHERE o.user_id = ?
        """;
  }

  private RowMapper<OrderDto> mapper() {
    return (rs, rowNum) -> mapOrder(rs);
  }

  private OrderDto mapOrder(ResultSet rs) throws SQLException {
    int priceCent = rs.getInt("price_cent");
    var appointmentAt = rs.getTimestamp("appointment_at").toLocalDateTime();
    var createdAt = rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString();
    return new OrderDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("order_no"),
        String.valueOf(rs.getLong("service_id")),
        rs.getString("service_name"),
        rs.getString("service_cover_url"),
        String.valueOf(rs.getLong("store_id")),
        rs.getString("store_name"),
        rs.getString("store_address"),
        priceCent / 100,
        priceCent,
        rs.getInt("duration_min"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        appointmentAt.toLocalDate().toString(),
        appointmentAt.toLocalTime().toString().substring(0, 5),
        rs.getString("status"),
        rs.getString("pay_status"),
        createdAt);
  }
}

