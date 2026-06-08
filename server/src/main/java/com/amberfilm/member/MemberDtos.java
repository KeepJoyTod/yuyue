package com.amberfilm.member;

import java.util.List;

public final class MemberDtos {
  private MemberDtos() {
  }

  public record MemberAssetSummaryDto(
      int couponCount,
      int pointBalance,
      int balanceCent,
      int cardCount) {
  }

  public record MemberCouponDto(
      String id,
      String title,
      String couponType,
      int amountCent,
      int thresholdCent,
      String status,
      String validFrom,
      String expiresAt,
      String source,
      String createdAt) {
  }

  public record PointTransactionDto(
      String id,
      String type,
      int deltaPoints,
      int balanceAfter,
      String source,
      String sourceOrderId,
      String reason,
      String createdAt) {
  }

  public record WalletTransactionDto(
      String id,
      String type,
      int deltaCent,
      int balanceAfterCent,
      String source,
      String sourceOrderId,
      String reason,
      String createdAt) {
  }

  public record MemberCardDto(
      String id,
      String title,
      String applicableServiceId,
      String applicableServiceName,
      int totalTimes,
      int remainingTimes,
      String status,
      String validFrom,
      String expiresAt,
      String source,
      String createdAt) {
  }

  public record AdminMemberAssetsDto(
      MemberAssetSummaryDto summary,
      List<MemberCouponDto> coupons,
      List<MemberCardDto> cards,
      List<PointTransactionDto> pointTransactions,
      List<WalletTransactionDto> walletTransactions) {
  }

  public record GrantCouponRequest(
      String title,
      String couponType,
      Integer amountCent,
      Integer thresholdCent,
      String validFrom,
      String expiresAt,
      String reason) {
  }

  public record AdjustPointsRequest(Integer deltaPoints, String reason) {
  }

  public record AdjustWalletRequest(Integer deltaCent, String reason) {
  }

  public record GrantCardRequest(
      String title,
      Long applicableServiceId,
      Integer totalTimes,
      String validFrom,
      String expiresAt,
      String reason) {
  }

  public record AdjustCardTimesRequest(Integer deltaTimes, String reason) {
  }

  public record ReasonRequest(String reason) {
  }
}
