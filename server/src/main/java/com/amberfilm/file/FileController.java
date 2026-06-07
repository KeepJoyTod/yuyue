package com.amberfilm.file;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileController {
  private final AuthService authService;
  private final FileService fileService;

  public FileController(AuthService authService, FileService fileService) {
    this.authService = authService;
    this.fileService = fileService;
  }

  @PostMapping("/api/files/upload-token")
  public ApiResponse<UploadTokenDto> uploadToken(HttpServletRequest request, @Valid @RequestBody UploadTokenRequest body) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(fileService.createUploadToken(userId, body));
  }

  @GetMapping("/api/files/{id}/download-url")
  public ApiResponse<DownloadUrlDto> downloadUrl(HttpServletRequest request, @PathVariable long id) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(fileService.createDownloadUrl(userId, id));
  }

  @PutMapping("/api/files/local-upload")
  public ApiResponse<Void> localUpload(
      HttpServletRequest request,
      @RequestParam String objectKey,
      @RequestParam long expiresAt,
      @RequestParam String signature) throws IOException {
    fileService.saveLocalUpload(objectKey, expiresAt, signature, request.getInputStream());
    return ApiResponse.ok(null);
  }

  @GetMapping("/api/files/local-download")
  public ResponseEntity<Resource> localDownload(
      @RequestParam String objectKey,
      @RequestParam long expiresAt,
      @RequestParam String signature) {
    FileService.LocalDownload download = fileService.loadLocalDownload(objectKey, expiresAt, signature);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(download.contentType()))
        .body(download.resource());
  }
}
