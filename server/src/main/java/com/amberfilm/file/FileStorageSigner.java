package com.amberfilm.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileStorageSigner {
  private final String provider;
  private final String bucket;
  private final String publicBaseUrl;
  private final String uploadBaseUrl;
  private final String signingSecret;
  private final long uploadTtlSeconds;
  private final long downloadTtlSeconds;

  public FileStorageSigner(
      @Value("${amber.storage.provider:local}") String provider,
      @Value("${amber.storage.bucket:amber-film-dev}") String bucket,
      @Value("${amber.storage.public-base-url:http://localhost:8080}") String publicBaseUrl,
      @Value("${amber.storage.upload-base-url:http://localhost:8080}") String uploadBaseUrl,
      @Value("${amber.storage.signing-secret:${amber.auth.token-secret}}") String signingSecret,
      @Value("${amber.storage.upload-ttl-seconds:900}") long uploadTtlSeconds,
      @Value("${amber.storage.download-ttl-seconds:900}") long downloadTtlSeconds) {
    this.provider = provider;
    this.bucket = bucket;
    this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
    this.uploadBaseUrl = trimTrailingSlash(uploadBaseUrl);
    this.signingSecret = signingSecret;
    this.uploadTtlSeconds = uploadTtlSeconds;
    this.downloadTtlSeconds = downloadTtlSeconds;
  }

  public String provider() {
    return provider;
  }

  public String bucket() {
    return bucket;
  }

  public SignedUrl uploadUrl(String objectKey) {
    return signedUrl(uploadBaseUrl + "/api/files/local-upload", objectKey, uploadTtlSeconds);
  }

  public SignedUrl downloadUrl(String objectKey) {
    return signedUrl(publicBaseUrl + "/api/files/local-download", objectKey, downloadTtlSeconds);
  }

  public boolean verify(String objectKey, long expiresAt, String signature) {
    if (Instant.now().getEpochSecond() > expiresAt || signature == null || signature.isBlank()) {
      return false;
    }
    return sign(objectKey + ":" + expiresAt).equals(signature);
  }

  private SignedUrl signedUrl(String endpoint, String objectKey, long ttlSeconds) {
    long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
    String signature = sign(objectKey + ":" + expiresAt);
    String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
    String encodedSignature = URLEncoder.encode(signature, StandardCharsets.UTF_8);
    String url = endpoint + "?objectKey=" + encodedKey + "&expiresAt=" + expiresAt + "&signature=" + encodedSignature;
    return new SignedUrl(url, OffsetDateTime.ofInstant(Instant.ofEpochSecond(expiresAt), ZoneOffset.UTC).toString());
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("File URL signing failed", ex);
    }
  }

  private static String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  public record SignedUrl(String url, String expiresAt) {
  }
}
