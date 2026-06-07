package com.amberfilm.negative;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NegativeController {
  private final AuthService authService;
  private final JdbcTemplate jdbcTemplate;

  public NegativeController(AuthService authService, JdbcTemplate jdbcTemplate) {
    this.authService = authService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/api/negatives")
  public ApiResponse<List<NegativeDto>> list(HttpServletRequest request) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(jdbcTemplate.query("""
        SELECT id, order_id, title, type, image_url, created_at
        FROM negatives
        WHERE user_id = ? AND status = 'visible'
        ORDER BY created_at DESC
        """, (rs, rowNum) -> new NegativeDto(
        String.valueOf(rs.getLong("id")),
        String.valueOf(rs.getLong("order_id")),
        rs.getString("title"),
        rs.getString("type"),
        rs.getString("image_url"),
        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString()), userId));
  }
}

