package com.amberfilm.file;

import com.amberfilm.common.ApiException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FileService {
  private static final long MAX_IMAGE_SIZE_BYTE = 20L * 1024 * 1024;

  public UploadTokenDto createUploadToken(long userId, UploadTokenRequest request) {
    validateImage(request);
    String objectKey = "uploads/users/%d/%s/%s-%s".formatted(
        userId,
        OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().toString().replace("-", ""),
        UUID.randomUUID(),
        sanitizeFileName(request.fileName()));
    String encodedObjectKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
    String assetUrl = "mock://amber-film/" + objectKey;
    return new UploadTokenDto(
        "mock-local",
        objectKey,
        "/api/files/mock-upload?objectKey=" + encodedObjectKey,
        assetUrl,
        "PUT",
        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15).toString());
  }

  private void validateImage(UploadTokenRequest request) {
    String contentType = request.contentType().toLowerCase(Locale.ROOT);
    if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
      throw ApiException.badRequest("FILE_TYPE_UNSUPPORTED", "仅支持 jpeg、png、webp 图片");
    }
    if (request.sizeByte() > MAX_IMAGE_SIZE_BYTE) {
      throw ApiException.badRequest("FILE_TOO_LARGE", "图片大小不能超过 20MB");
    }
  }

  private String sanitizeFileName(String fileName) {
    String sanitized = fileName.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    return sanitized.isBlank() ? "upload.bin" : sanitized;
  }
}
