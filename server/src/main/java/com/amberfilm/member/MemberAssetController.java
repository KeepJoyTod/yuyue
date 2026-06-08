package com.amberfilm.member;

import com.amberfilm.auth.AuthService;
import com.amberfilm.common.ApiResponse;
import com.amberfilm.member.MemberDtos.MemberCardDto;
import com.amberfilm.member.MemberDtos.MemberCouponDto;
import com.amberfilm.member.MemberDtos.PointTransactionDto;
import com.amberfilm.member.MemberDtos.WalletTransactionDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberAssetController {
  private final AuthService authService;
  private final MemberAssetService memberAssetService;

  public MemberAssetController(AuthService authService, MemberAssetService memberAssetService) {
    this.authService = authService;
    this.memberAssetService = memberAssetService;
  }

  @GetMapping("/api/users/me/coupons")
  public ApiResponse<List<MemberCouponDto>> coupons(
      HttpServletRequest request,
      @RequestParam(required = false) String status) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(memberAssetService.coupons(userId, status));
  }

  @GetMapping("/api/users/me/points/transactions")
  public ApiResponse<List<PointTransactionDto>> pointTransactions(
      HttpServletRequest request,
      @RequestParam(required = false) Integer limit) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(memberAssetService.pointTransactions(userId, limit));
  }

  @GetMapping("/api/users/me/wallet/transactions")
  public ApiResponse<List<WalletTransactionDto>> walletTransactions(
      HttpServletRequest request,
      @RequestParam(required = false) Integer limit) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(memberAssetService.walletTransactions(userId, limit));
  }

  @GetMapping("/api/users/me/cards")
  public ApiResponse<List<MemberCardDto>> cards(
      HttpServletRequest request,
      @RequestParam(required = false) String status) {
    long userId = authService.requireUserId(request);
    return ApiResponse.ok(memberAssetService.cards(userId, status));
  }
}
