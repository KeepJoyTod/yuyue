package com.amberfilm.user;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
  private final AuthService authService;
  private final UserSummaryService userSummaryService;

  public UserController(AuthService authService, UserSummaryService userSummaryService) {
    this.authService = authService;
    this.userSummaryService = userSummaryService;
  }

  @GetMapping("/api/users/me/summary")
  public ApiResponse<UserSummaryDto> summary(HttpServletRequest request) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(userSummaryService.summary(userId));
  }
}
