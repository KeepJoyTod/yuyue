package com.amberfilm.user;

public record UserSummaryDto(
    String levelName,
    String nextLevelName,
    int growth,
    int nextNeed,
    int couponCount,
    int pointBalance,
    int balanceCent,
    int cardCount,
    int orderCount,
    int completedOrderCount) {
}
