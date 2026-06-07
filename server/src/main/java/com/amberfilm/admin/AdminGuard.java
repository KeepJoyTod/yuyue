package com.amberfilm.admin;

import com.amberfilm.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {
  private final String adminToken;

  public AdminGuard(@Value("${amber.admin.token:dev-admin-token}") String adminToken) {
    this.adminToken = adminToken;
  }

  public String requireAdmin(String token) {
    if (adminToken == null || adminToken.isBlank() || token == null || !adminToken.equals(token)) {
      throw new ApiException("ADMIN_AUTH_REQUIRED", "管理员鉴权失败", HttpStatus.UNAUTHORIZED);
    }
    return digestToken(token);
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
