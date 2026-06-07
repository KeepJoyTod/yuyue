package com.amberfilm.auth;

import com.amberfilm.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final String secret;
  private final long ttlSeconds;

  public TokenService(
      @Value("${amber.auth.token-secret}") String secret,
      @Value("${amber.auth.token-ttl-seconds}") long ttlSeconds) {
    this.secret = secret;
    this.ttlSeconds = ttlSeconds;
  }

  public String issue(long userId) {
    long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
    String payload = userId + ":" + expiresAt + ":" + Long.toHexString(Double.doubleToLongBits(Math.random()));
    String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
    String signature = sign(encodedPayload);
    return encodedPayload + "." + signature;
  }

  public long verify(String token) {
    if (token == null || token.isBlank()) {
      throw ApiException.unauthorized("请先登录");
    }
    String[] parts = token.split("\\.");
    if (parts.length != 2) {
      throw ApiException.unauthorized("登录状态无效");
    }
    String expected = sign(parts[0]);
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
      throw ApiException.unauthorized("登录状态无效");
    }
    String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String[] values = payload.split(":");
    if (values.length < 2) {
      throw ApiException.unauthorized("登录状态无效");
    }
    long expiresAt = Long.parseLong(values[1]);
    if (expiresAt < Instant.now().getEpochSecond()) {
      throw ApiException.unauthorized("登录已过期");
    }
    return Long.parseLong(values[0]);
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Token sign failed", ex);
    }
  }

  private static String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}

