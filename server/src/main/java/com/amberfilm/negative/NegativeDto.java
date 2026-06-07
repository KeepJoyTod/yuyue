package com.amberfilm.negative;

public record NegativeDto(
    String id,
    String orderId,
    String title,
    String type,
    String imageUrl,
    String fileId,
    String downloadUrl,
    String createdAt) {
}
