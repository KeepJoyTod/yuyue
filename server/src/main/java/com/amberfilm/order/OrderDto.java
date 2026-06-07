package com.amberfilm.order;

public record OrderDto(
    String id,
    String orderNo,
    String serviceId,
    String serviceName,
    String serviceCoverUrl,
    String storeId,
    String storeName,
    String storeAddress,
    int price,
    int priceCent,
    int durationMin,
    String contactName,
    String contactPhone,
    String date,
    String time,
    String status,
    String payStatus,
    String createdAt) {
}

