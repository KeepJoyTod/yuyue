package com.amberfilm.file;

import com.amberfilm.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileService {
  private static final long MAX_IMAGE_SIZE_BYTE = 20L * 1024 * 1024;
  private final JdbcTemplate jdbcTemplate;
  private final FileStorageSigner storageSigner;
  private final Path localStorageRoot;

  public FileService(
      JdbcTemplate jdbcTemplate,
      FileStorageSigner storageSigner,
      @Value("${amber.storage.local-root:./data/uploads}") String localStorageRoot) {
    this.jdbcTemplate = jdbcTemplate;
    this.storageSigner = storageSigner;
    this.localStorageRoot = Path.of(localStorageRoot).toAbsolutePath().normalize();
  }

  @Transactional
  public UploadTokenDto createUploadToken(long userId, UploadTokenRequest request) {
    validateImage(request);
    String objectKey = "uploads/users/%d/%s/%s-%s".formatted(
        userId,
        OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().toString().replace("-", ""),
        UUID.randomUUID(),
        sanitizeFileName(request.fileName()));
    long fileId = createFileRecord(userId, objectKey, request);
    FileStorageSigner.SignedUrl upload = storageSigner.uploadUrl(objectKey);
    FileStorageSigner.SignedUrl download = storageSigner.downloadUrl(objectKey);
    return new UploadTokenDto(
        String.valueOf(fileId),
        storageSigner.provider(),
        storageSigner.bucket(),
        objectKey,
        upload.url(),
        download.url(),
        "PUT",
        upload.expiresAt());
  }

  public DownloadUrlDto createDownloadUrl(long userId, long fileId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT id, object_key
        FROM files
        WHERE id = ? AND owner_user_id = ?
        """, fileId, userId);
    if (rows.isEmpty()) {
      throw ApiException.notFound("文件不存在或无权访问");
    }
    FileStorageSigner.SignedUrl signedUrl = storageSigner.downloadUrl((String) rows.get(0).get("object_key"));
    return new DownloadUrlDto(String.valueOf(fileId), signedUrl.url(), signedUrl.expiresAt());
  }

  public String signedDownloadUrl(String objectKey) {
    return storageSigner.downloadUrl(objectKey).url();
  }

  public void saveLocalUpload(String objectKey, long expiresAt, String signature, InputStream inputStream) throws IOException {
    verifySignedAccess(objectKey, expiresAt, signature);
    Path target = localPath(objectKey);
    Files.createDirectories(target.getParent());
    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    jdbcTemplate.update("""
        UPDATE files
        SET status = 'uploaded', updated_at = CURRENT_TIMESTAMP
        WHERE object_key = ? AND storage_provider = ? AND bucket = ?
        """, objectKey, storageSigner.provider(), storageSigner.bucket());
  }

  public LocalDownload loadLocalDownload(String objectKey, long expiresAt, String signature) {
    verifySignedAccess(objectKey, expiresAt, signature);
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
        SELECT mime_type
        FROM files
        WHERE object_key = ? AND storage_provider = ? AND bucket = ?
        """, objectKey, storageSigner.provider(), storageSigner.bucket());
    if (rows.isEmpty()) {
      throw ApiException.notFound("文件不存在");
    }
    Path path = localPath(objectKey);
    if (!Files.exists(path)) {
      throw ApiException.notFound("本地文件尚未上传");
    }
    return new LocalDownload(new FileSystemResource(path), (String) rows.get(0).get("mime_type"));
  }

  private long createFileRecord(long userId, String objectKey, UploadTokenRequest request) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement("""
          INSERT INTO files(owner_user_id, storage_provider, bucket, object_key, public_url, mime_type, size_byte, usage, status)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'created')
          """, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, userId);
      ps.setString(2, storageSigner.provider());
      ps.setString(3, storageSigner.bucket());
      ps.setString(4, objectKey);
      ps.setString(5, null);
      ps.setString(6, request.contentType());
      ps.setLong(7, request.sizeByte());
      ps.setString(8, normalizeUsage(request.usage()));
      return ps;
    }, keyHolder);
    List<Map<String, Object>> keys = keyHolder.getKeyList();
    if (keys.isEmpty()) {
      throw new IllegalStateException("Generated file id is missing");
    }
    Object id = keys.get(0).get("id");
    if (id == null && keys.get(0).size() == 1) {
      id = keys.get(0).values().iterator().next();
    }
    if (!(id instanceof Number number)) {
      throw new IllegalStateException("Generated file id is missing");
    }
    return number.longValue();
  }

  private void verifySignedAccess(String objectKey, long expiresAt, String signature) {
    if (!storageSigner.verify(objectKey, expiresAt, signature)) {
      throw ApiException.badRequest("FILE_SIGNATURE_INVALID", "文件 URL 签名无效或已过期");
    }
  }

  private Path localPath(String objectKey) {
    Path path = localStorageRoot.resolve(objectKey).normalize();
    if (!path.startsWith(localStorageRoot)) {
      throw ApiException.badRequest("FILE_OBJECT_KEY_INVALID", "文件路径不合法");
    }
    return path;
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

  private String normalizeUsage(String usage) {
    String normalized = usage.toLowerCase(Locale.ROOT);
    if (!normalized.equals("negative") && !normalized.equals("service-cover") && !normalized.equals("store-cover")) {
      throw ApiException.badRequest("FILE_USAGE_INVALID", "文件用途必须是 negative、service-cover 或 store-cover");
    }
    return normalized;
  }

  private String sanitizeFileName(String fileName) {
    String sanitized = fileName.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    return sanitized.isBlank() ? "upload.bin" : sanitized;
  }

  public record LocalDownload(Resource resource, String contentType) {
  }
}
