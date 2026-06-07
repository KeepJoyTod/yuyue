package com.amberfilm.auth;

import com.amberfilm.common.ApiException;
import com.amberfilm.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final JdbcTemplate jdbcTemplate;
  private final TokenService tokenService;
  private final boolean devSmsEnabled;
  private final long smsTtlSeconds;
  private final Random random = new Random();

  public AuthService(
      JdbcTemplate jdbcTemplate,
      TokenService tokenService,
      @Value("${amber.auth.dev-sms-enabled:true}") boolean devSmsEnabled,
      @Value("${amber.auth.sms-ttl-seconds:300}") long smsTtlSeconds) {
    this.jdbcTemplate = jdbcTemplate;
    this.tokenService = tokenService;
    this.devSmsEnabled = devSmsEnabled;
    this.smsTtlSeconds = smsTtlSeconds;
  }

  @Transactional
  public LoginResponse phoneLogin(PhoneLoginRequest request) {
    verifySmsCode(request.phone(), "login", request.code());
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

  @Transactional
  public SmsSendResponse sendSms(SmsSendRequest request) {
    String scene = normalizeScene(request.scene());
    String code = "%06d".formatted(random.nextInt(1_000_000));
    Instant expiresAt = Instant.now().plusSeconds(smsTtlSeconds);
    jdbcTemplate.update("""
        INSERT INTO sms_codes(phone, code_hash, scene, expires_at)
        VALUES (?, ?, ?, ?)
        """, request.phone(), hashPlain(code), scene, Timestamp.from(expiresAt));
    return new SmsSendResponse(
        request.phone(),
        scene,
        OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC).toString(),
        devSmsEnabled ? code : null);
  }

  @Transactional
  public LoginResponse wechatLogin(WechatLoginRequest request) {
    String openid = "dev-wx-" + hashPlain(request.code()).substring(0, 24);
    Long userId = findUserIdByOpenid(openid);
    if (userId == null) {
      jdbcTemplate.update(
          "INSERT INTO users(openid, phone, nickname, status) VALUES (?, ?, ?, 'normal')",
          openid,
          request.phone(),
          "微信用户");
      userId = findUserIdByOpenid(openid);
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

  private Long findUserIdByOpenid(String openid) {
    List<Long> ids = jdbcTemplate.query(
        "SELECT id FROM users WHERE openid = ?",
        (rs, rowNum) -> rs.getLong("id"),
        openid);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private void verifySmsCode(String phone, String scene, String code) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, code_hash
        FROM sms_codes
        WHERE phone = ? AND scene = ? AND consumed_at IS NULL AND expires_at > ?
        ORDER BY created_at DESC
        """, phone, scene, Timestamp.from(Instant.now()));
    if (rows.isEmpty()) {
      if (devSmsEnabled && "000000".equals(code)) {
        return;
      }
      throw ApiException.badRequest("SMS_CODE_INVALID", "验证码无效或已过期");
    }
    Map<String, Object> row = rows.get(0);
    if (!hashPlain(code).equals(row.get("code_hash"))) {
      throw ApiException.badRequest("SMS_CODE_INVALID", "验证码无效或已过期");
    }
    jdbcTemplate.update("UPDATE sms_codes SET consumed_at = CURRENT_TIMESTAMP WHERE id = ?", row.get("id"));
  }

  private String normalizeScene(String scene) {
    if (scene == null || scene.isBlank()) {
      return "login";
    }
    if (!"login".equals(scene)) {
      throw ApiException.badRequest("SMS_SCENE_INVALID", "短信场景暂仅支持 login");
    }
    return scene;
  }

  private static String hashNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return hashPlain(value);
  }

  private static String hashPlain(String value) {
    byte[] digest;
    try {
      digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
    return HexFormat.of().formatHex(digest);
  }
}
