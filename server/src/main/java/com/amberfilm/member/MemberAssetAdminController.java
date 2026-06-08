package com.amberfilm.member;

import com.amberfilm.admin.AdminGuard;
import com.amberfilm.admin.AdminPrincipal;
import com.amberfilm.common.ApiResponse;
import com.amberfilm.member.MemberDtos.AdjustCardTimesRequest;
import com.amberfilm.member.MemberDtos.AdjustPointsRequest;
import com.amberfilm.member.MemberDtos.AdjustWalletRequest;
import com.amberfilm.member.MemberDtos.AdminMemberAssetsDto;
import com.amberfilm.member.MemberDtos.GrantCardRequest;
import com.amberfilm.member.MemberDtos.GrantCouponRequest;
import com.amberfilm.member.MemberDtos.MemberCardDto;
import com.amberfilm.member.MemberDtos.MemberCouponDto;
import com.amberfilm.member.MemberDtos.PointTransactionDto;
import com.amberfilm.member.MemberDtos.ReasonRequest;
import com.amberfilm.member.MemberDtos.WalletTransactionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberAssetAdminController {
  private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

  private final AdminGuard adminGuard;
  private final MemberAssetService memberAssetService;

  public MemberAssetAdminController(AdminGuard adminGuard, MemberAssetService memberAssetService) {
    this.adminGuard = adminGuard;
    this.memberAssetService = memberAssetService;
  }

  @GetMapping("/api/admin/users/{userId}/assets")
  public ApiResponse<AdminMemberAssetsDto> assets(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long userId) {
    adminGuard.requireAdmin(token);
    return ApiResponse.ok(memberAssetService.adminAssets(userId));
  }

  @PostMapping("/api/admin/users/{userId}/coupons")
  public ApiResponse<MemberCouponDto> grantCoupon(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long userId,
      @RequestBody GrantCouponRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.grantCoupon(principal.tokenDigest(), userId, body));
  }

  @PostMapping("/api/admin/coupons/{id}/void")
  public ApiResponse<MemberCouponDto> voidCoupon(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long id,
      @RequestBody(required = false) ReasonRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.voidCoupon(principal.tokenDigest(), id, body == null ? null : body.reason()));
  }

  @PostMapping("/api/admin/users/{userId}/points/adjust")
  public ApiResponse<PointTransactionDto> adjustPoints(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long userId,
      @RequestBody AdjustPointsRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.adjustPoints(principal.tokenDigest(), userId, body));
  }

  @PostMapping("/api/admin/users/{userId}/wallet/adjust")
  public ApiResponse<WalletTransactionDto> adjustWallet(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long userId,
      @RequestBody AdjustWalletRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.adjustWallet(principal.tokenDigest(), userId, body));
  }

  @PostMapping("/api/admin/users/{userId}/cards")
  public ApiResponse<MemberCardDto> grantCard(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long userId,
      @RequestBody GrantCardRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.grantCard(principal.tokenDigest(), userId, body));
  }

  @PostMapping("/api/admin/cards/{id}/adjust-times")
  public ApiResponse<MemberCardDto> adjustCardTimes(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long id,
      @RequestBody AdjustCardTimesRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.adjustCardTimes(principal.tokenDigest(), id, body));
  }

  @PostMapping("/api/admin/cards/{id}/void")
  public ApiResponse<MemberCardDto> voidCard(
      @RequestHeader(name = ADMIN_TOKEN_HEADER, required = false) String token,
      @PathVariable long id,
      @RequestBody(required = false) ReasonRequest body) {
    AdminPrincipal principal = adminGuard.requireWritableAdmin(token);
    return ApiResponse.ok(memberAssetService.voidCard(principal.tokenDigest(), id, body == null ? null : body.reason()));
  }
}
