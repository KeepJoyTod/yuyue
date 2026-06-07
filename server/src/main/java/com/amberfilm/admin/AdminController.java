package com.amberfilm.admin;

import com.amberfilm.admin.AdminDtos.AdminAuditLogDto;
import com.amberfilm.admin.AdminDtos.AdminNegativeDto;
import com.amberfilm.admin.AdminDtos.AdminOrderDto;
import com.amberfilm.admin.AdminDtos.AdminScheduleDto;
import com.amberfilm.admin.AdminDtos.AdminServiceDto;
import com.amberfilm.admin.AdminDtos.AdminStoreDto;
import com.amberfilm.admin.AdminDtos.AdminSummaryDto;
import com.amberfilm.common.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
  private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

  private final AdminGuard adminGuard;
  private final AdminService adminService;

  public AdminController(AdminGuard adminGuard, AdminService adminService) {
    this.adminGuard = adminGuard;
    this.adminService = adminService;
  }

  @GetMapping("/api/admin/summary")
  public ApiResponse<AdminSummaryDto> summary(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.summary());
  }

  @GetMapping("/api/admin/services")
  public ApiResponse<List<AdminServiceDto>> services(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.services());
  }

  @GetMapping("/api/admin/stores")
  public ApiResponse<List<AdminStoreDto>> stores(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.stores());
  }

  @GetMapping("/api/admin/schedules")
  public ApiResponse<List<AdminScheduleDto>> schedules(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.schedules(date));
  }

  @GetMapping("/api/admin/orders")
  public ApiResponse<List<AdminOrderDto>> orders(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @RequestParam(required = false) String status) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.orders(status));
  }

  @GetMapping("/api/admin/negatives")
  public ApiResponse<List<AdminNegativeDto>> negatives(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long orderId) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.negatives(userId, orderId));
  }

  @PostMapping("/api/admin/negatives")
  public ApiResponse<AdminNegativeDto> createNegative(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @Valid @RequestBody CreateAdminNegativeRequest body) {
    String adminTokenDigest = adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.createNegative(adminTokenDigest, body));
  }

  @GetMapping("/api/admin/audit-logs")
  public ApiResponse<List<AdminAuditLogDto>> auditLogs(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String targetType,
      @RequestParam(required = false) Long targetId) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(adminService.auditLogs(action, targetType, targetId));
  }
}
