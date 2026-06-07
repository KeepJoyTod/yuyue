package com.amberfilm.auth;

import com.amberfilm.common.ApiResponse;
import com.amberfilm.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/api/auth/phone-login")
  public ApiResponse<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
    return ApiResponse.ok(authService.phoneLogin(request));
  }

  @GetMapping("/api/users/me")
  public ApiResponse<UserDto> me(HttpServletRequest request) {
    return ApiResponse.ok(authService.requireUser(request));
  }

  @PostMapping("/api/users/real-name")
  public ApiResponse<UserDto> realName(HttpServletRequest request, @Valid @RequestBody RealNameRequest body) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(authService.updateRealName(userId, body));
  }
}

