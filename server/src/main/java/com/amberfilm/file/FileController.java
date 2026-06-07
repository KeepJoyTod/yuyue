package com.amberfilm.file;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
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
}
