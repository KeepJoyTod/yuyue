package com.amberfilm.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public StoreService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public List<StoreDto> stores(String keyword, String tag, Long serviceId) {
    StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT st.id, st.name, st.address, st.distance_km, st.rating, st.reviews,
               st.hours, st.tags_json, st.cover_url,
               CASE WHEN EXISTS (
                 SELECT 1 FROM schedules sc
                 WHERE sc.store_id = st.id
                   AND sc.status = 'available'
                   AND sc.booked_count < sc.capacity
                   AND sc.service_date = CURRENT_DATE
               ) THEN TRUE ELSE FALSE END AS has_slot_today
        FROM stores st
        """);
    List<Object> params = new ArrayList<>();
    if (serviceId != null) {
      sql.append("JOIN store_service_rel ssr ON ssr.store_id = st.id AND ssr.enabled = TRUE AND ssr.service_id = ? ");
      params.add(serviceId);
    }
    sql.append("WHERE st.enabled = TRUE ");
    if (keyword != null && !keyword.isBlank()) {
      sql.append("AND (LOWER(st.name) LIKE ? OR LOWER(st.address) LIKE ?) ");
      String kw = "%" + keyword.trim().toLowerCase() + "%";
      params.add(kw);
      params.add(kw);
    }
    sql.append("ORDER BY st.id");
    List<StoreDto> stores = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new StoreDto(
        String.valueOf(rs.getLong("id")),
        rs.getString("name"),
        rs.getString("address"),
        rs.getBigDecimal("distance_km"),
        rs.getBigDecimal("rating"),
        rs.getInt("reviews"),
        rs.getString("hours"),
        parseTags(rs.getString("tags_json")),
        rs.getString("cover_url"),
        rs.getBoolean("has_slot_today")), params.toArray());

    if (tag == null || tag.isBlank() || "all".equals(tag)) {
      return stores;
    }
    return stores.stream().filter(store -> store.tags().contains(tag)).toList();
  }

  public List<ScheduleDayDto> schedules(long storeId, Long serviceId, LocalDate startDate, int days) {
    LocalDate start = startDate == null ? LocalDate.now() : startDate;
    int normalizedDays = Math.max(1, Math.min(days <= 0 ? 7 : days, 30));
    LocalDate end = start.plusDays(normalizedDays - 1L);
    String sql = """
        SELECT id, service_date, start_time, capacity, booked_count, status
        FROM schedules
        WHERE store_id = ?
          AND service_date BETWEEN ? AND ?
          AND (? IS NULL OR service_id = ?)
        ORDER BY service_date, start_time
        """;
    Map<String, List<ScheduleSlotDto>> byDate = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      String date = rs.getDate("service_date").toLocalDate().toString();
      int remaining = Math.max(0, rs.getInt("capacity") - rs.getInt("booked_count"));
      boolean available = "available".equals(rs.getString("status")) && remaining > 0;
      byDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(new ScheduleSlotDto(
          String.valueOf(rs.getLong("id")),
          rs.getTime("start_time").toLocalTime().toString().substring(0, 5),
          available,
          remaining));
    }, storeId, Date.valueOf(start), Date.valueOf(end), serviceId, serviceId);
    return byDate.entrySet().stream().map(entry -> new ScheduleDayDto(entry.getKey(), entry.getValue())).toList();
  }

  private List<String> parseTags(String tagsJson) {
    if (tagsJson == null || tagsJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(tagsJson, new TypeReference<>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }
}

