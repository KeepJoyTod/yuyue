package com.amberfilm.auth;

import com.amberfilm.common.ApiException;
import com.amberfilm.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final JdbcTemplate jdbcTemplate;
  private final TokenService tokenService;

  public AuthService(JdbcTemplate jdbcTemplate, TokenService tokenService) {
    this.jdbcTemplate = jdbcTemplate;
    this.tokenService = tokenService;
  }

  @Transactional
  public LoginResponse phoneLogin(PhoneLoginRequest request) {
    Long userId = findUserIdByPhone(request.phone());
    if (userId == null) {
      jdbcTemplate.update(
          "INSERT INTO users(phone, nickname, status) VALUES (?, ?, 'normal')",
          request.phone(),
          "微信用户");
      userId = findUserIdByPhone(request.phone());
    }
    UserDto user = getUser(userId);
    return new LoginResponse(tokenService.issue(user.id()), user);
  }

  public UserDto requireUser(HttpServletRequest request) {
    return getUser(requireUserId(request));
  }

  public long requireUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    return tokenService.verify(token);
  }

  public UserDto getUser(long userId) {
    List<UserDto> users = jdbcTemplate.query(
        "SELECT id, nickname, phone, real_name, avatar_url FROM users WHERE id = ? AND status = 'normal'",
        (rs, rowNum) -> new UserDto(
            rs.getLong("id"),
            rs.getString("nickname"),
            rs.getString("phone"),
            rs.getString("real_name"),
            rs.getString("avatar_url")),
        userId);
    if (users.isEmpty()) {
      throw new ApiException("USER_NOT_FOUND", "用户不存在或已禁用", HttpStatus.UNAUTHORIZED);
    }
    return users.get(0);
  }

  @Transactional
  public UserDto updateRealName(long userId, RealNameRequest request) {
    jdbcTemplate.update(
        "UPDATE users SET real_name = ?, id_card_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        request.realName(),
        hashNullable(request.idCardNo()),
        userId);
    return getUser(userId);
  }

  private Long findUserIdByPhone(String phone) {
    List<Long> ids = jdbcTemplate.query(
        "SELECT id FROM users WHERE phone = ?",
        (rs, rowNum) -> rs.getLong("id"),
        phone);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private static String hashNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    byte[] digest;
    try {
      digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
    return HexFormat.of().formatHex(digest);
  }
}

