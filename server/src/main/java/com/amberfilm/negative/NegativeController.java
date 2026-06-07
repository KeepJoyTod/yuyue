package com.amberfilm.negative;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import com.amberfilm.file.FileService;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NegativeController {
  private final AuthService authService;
  private final JdbcTemplate jdbcTemplate;
  private final FileService fileService;

  public NegativeController(AuthService authService, JdbcTemplate jdbcTemplate, FileService fileService) {
    this.authService = authService;
    this.jdbcTemplate = jdbcTemplate;
    this.fileService = fileService;
  }

  @GetMapping("/api/negatives")
  public ApiResponse<List<NegativeDto>> list(HttpServletRequest request) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(jdbcTemplate.query("""
        SELECT n.id, n.order_id, n.title, n.type, n.image_url, n.file_id, f.object_key, n.created_at
        FROM negatives n
        LEFT JOIN files f ON f.id = n.file_id
        WHERE n.user_id = ? AND n.status = 'visible'
        ORDER BY n.created_at DESC
        """, (rs, rowNum) -> {
      String objectKey = rs.getString("object_key");
      String legacyImageUrl = rs.getString("image_url");
      String downloadUrl = objectKey == null ? legacyImageUrl : fileService.signedDownloadUrl(objectKey);
      return new NegativeDto(
        String.valueOf(rs.getLong("id")),
        String.valueOf(rs.getLong("order_id")),
        rs.getString("title"),
        rs.getString("type"),
          legacyImageUrl,
          nullableId(rs, "file_id"),
          downloadUrl,
        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC).toString());
    }, userId));
  }

  private String nullableId(java.sql.ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : String.valueOf(value);
  }
}
