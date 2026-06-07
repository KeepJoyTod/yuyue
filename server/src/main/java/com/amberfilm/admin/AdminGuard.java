package com.amberfilm.admin;

import com.amberfilm.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {
  private final String adminToken;
  private final JdbcTemplate jdbcTemplate;

  public AdminGuard(
      @Value("${amber.admin.token:dev-admin-token}") String adminToken,
      JdbcTemplate jdbcTemplate) {
    this.adminToken = adminToken;
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  public void ensureBootstrapAdmin() {
    if (adminToken == null || adminToken.isBlank()) {
      return;
    }
    String digest = digestToken(adminToken);
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_users WHERE token_digest = ?", Integer.class, digest);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.update("""
        INSERT INTO admin_users(username, display_name, role, token_digest, status)
        VALUES (?, ?, 'super_admin', ?, 'normal')
        """, "bootstrap-admin", "Bootstrap Admin", digest);
  }

  public AdminPrincipal requireAdmin(String token) {
    if (token == null || token.isBlank()) {
      throw new ApiException("ADMIN_AUTH_REQUIRED", "管理员鉴权失败", HttpStatus.UNAUTHORIZED);
    }
    String digest = digestToken(token);
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, username, role
        FROM admin_users
        WHERE token_digest = ? AND status = 'normal'
        """, digest);
    if (rows.isEmpty()) {
      throw new ApiException("ADMIN_AUTH_REQUIRED", "管理员鉴权失败", HttpStatus.UNAUTHORIZED);
    }
    Map<String, Object> row = rows.get(0);
    return new AdminPrincipal(
        ((Number) row.get("id")).longValue(),
        (String) row.get("username"),
        (String) row.get("role"),
        digest);
  }

  public AdminPrincipal requireWritableAdmin(String token) {
    AdminPrincipal principal = requireAdmin(token);
    if (!principal.canWrite()) {
      throw new ApiException("ADMIN_FORBIDDEN", "管理员无写操作权限", HttpStatus.FORBIDDEN);
    }
    return principal;
  }

  private String digestToken(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
