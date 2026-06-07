package com.amberfilm.booking;

import com.amberfilm.common.ApiException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
  private final JdbcTemplate jdbcTemplate;

  public BookingService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public BookingCreatedDto create(long userId, CreateBookingRequest request) {
    Map<String, Object> service = singleRow(
        "SELECT id, price_cent FROM services WHERE id = ? AND enabled = TRUE",
        request.serviceId(),
        "服务不存在或已下架");
    Map<String, Object> schedule = singleRow("""
        SELECT id, store_id, service_id, service_date, start_time, capacity, booked_count, status
        FROM schedules
        WHERE id = ?
        FOR UPDATE
        """, request.scheduleId(), "档期不存在");

    long scheduleStoreId = ((Number) schedule.get("store_id")).longValue();
    Number scheduleServiceValue = (Number) schedule.get("service_id");
    Long scheduleServiceId = scheduleServiceValue == null ? null : scheduleServiceValue.longValue();
    if (scheduleStoreId != request.storeId()) {
      throw ApiException.badRequest("SCHEDULE_STORE_MISMATCH", "档期不属于所选门店");
    }
    if (scheduleServiceId != null && !scheduleServiceId.equals(request.serviceId())) {
      throw ApiException.badRequest("SCHEDULE_SERVICE_MISMATCH", "档期不支持所选套餐");
    }
    assertStoreServiceEnabled(request.storeId(), request.serviceId());

    int capacity = ((Number) schedule.get("capacity")).intValue();
    int bookedCount = ((Number) schedule.get("booked_count")).intValue();
    String status = (String) schedule.get("status");
    if (!"available".equals(status) || bookedCount >= capacity) {
      throw new ApiException("SCHEDULE_FULL", "档期已满", HttpStatus.CONFLICT);
    }

    jdbcTemplate.update(
        "UPDATE schedules SET booked_count = booked_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        request.scheduleId());

    Date serviceDate = (Date) schedule.get("service_date");
    Time startTime = (Time) schedule.get("start_time");
    Timestamp appointmentAt = Timestamp.valueOf(LocalDateTime.of(serviceDate.toLocalDate(), startTime.toLocalTime()));
    String orderNo = buildOrderNo();

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var ps = connection.prepareStatement("""
          INSERT INTO orders(order_no, user_id, service_id, store_id, schedule_id, contact_name, contact_phone,
                             price_cent, status, pay_status, appointment_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', 'unpaid', ?)
          """, new String[] {"id"});
      ps.setString(1, orderNo);
      ps.setLong(2, userId);
      ps.setLong(3, request.serviceId());
      ps.setLong(4, request.storeId());
      ps.setLong(5, request.scheduleId());
      ps.setString(6, request.contactName().trim());
      ps.setString(7, request.contactPhone().trim());
      ps.setInt(8, ((Number) service.get("price_cent")).intValue());
      ps.setTimestamp(9, appointmentAt);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Order id was not generated");
    }
    return new BookingCreatedDto(String.valueOf(key.longValue()), orderNo, "pending", "unpaid");
  }

  private void assertStoreServiceEnabled(long storeId, long serviceId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM store_service_rel WHERE store_id = ? AND service_id = ? AND enabled = TRUE",
        Integer.class,
        storeId,
        serviceId);
    if (count == null || count == 0) {
      throw ApiException.badRequest("STORE_SERVICE_UNAVAILABLE", "所选门店暂不支持该套餐");
    }
  }

  private Map<String, Object> singleRow(String sql, Object arg, String notFoundMessage) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, arg);
    if (rows.isEmpty()) {
      throw ApiException.notFound(notFoundMessage);
    }
    return rows.get(0);
  }

  private String buildOrderNo() {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    int tail = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "ORD" + time + tail;
  }
}

