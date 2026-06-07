package com.amberfilm.catalog;

import com.amberfilm.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public CatalogService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public List<CategoryDto> categories() {
    return jdbcTemplate.query(
        "SELECT id, code, name FROM service_categories WHERE enabled = TRUE ORDER BY sort_order, id",
        (rs, rowNum) -> new CategoryDto(
            String.valueOf(rs.getLong("id")),
            rs.getString("code"),
            rs.getString("name")));
  }

  public List<ServiceItemDto> services(String categoryCode, String keyword, Long storeId) {
    StringBuilder sql = new StringBuilder("""
        SELECT s.id, s.name, c.code AS category_code, c.name AS category_name, s.cover_url,
               s.price_cent, s.duration_min, s.description, s.tags_json, s.rating
        FROM services s
        JOIN service_categories c ON c.id = s.category_id
        """);
    List<Object> params = new ArrayList<>();
    if (storeId != null) {
      sql.append("JOIN store_service_rel ssr ON ssr.service_id = s.id AND ssr.enabled = TRUE AND ssr.store_id = ? ");
      params.add(storeId);
    }
    sql.append("WHERE s.enabled = TRUE AND c.enabled = TRUE ");
    if (categoryCode != null && !categoryCode.isBlank()) {
      sql.append("AND c.code = ? ");
      params.add(categoryCode);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append("AND (LOWER(s.name) LIKE ? OR LOWER(c.name) LIKE ?) ");
      String kw = "%" + keyword.trim().toLowerCase() + "%";
      params.add(kw);
      params.add(kw);
    }
    sql.append("ORDER BY s.id");
    return jdbcTemplate.query(sql.toString(), serviceMapper(), params.toArray());
  }

  public ServiceItemDto serviceDetail(long id) {
    List<ServiceItemDto> items = jdbcTemplate.query("""
        SELECT s.id, s.name, c.code AS category_code, c.name AS category_name, s.cover_url,
               s.price_cent, s.duration_min, s.description, s.tags_json, s.rating
        FROM services s
        JOIN service_categories c ON c.id = s.category_id
        WHERE s.enabled = TRUE AND c.enabled = TRUE AND s.id = ?
        """, serviceMapper(), id);
    if (items.isEmpty()) {
      throw ApiException.notFound("服务不存在或已下架");
    }
    return items.get(0);
  }

  private org.springframework.jdbc.core.RowMapper<ServiceItemDto> serviceMapper() {
    return (rs, rowNum) -> {
      int priceCent = rs.getInt("price_cent");
      return new ServiceItemDto(
          String.valueOf(rs.getLong("id")),
          rs.getString("name"),
          rs.getString("category_code"),
          rs.getString("category_name"),
          rs.getString("cover_url"),
          priceCent / 100,
          priceCent,
          rs.getInt("duration_min"),
          rs.getString("description"),
          parseTags(rs.getString("tags_json")),
          rs.getBigDecimal("rating"));
    };
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

