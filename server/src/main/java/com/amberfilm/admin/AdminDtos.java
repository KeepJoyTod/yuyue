package com.amberfilm.admin;

import java.math.BigDecimal;

public final class AdminDtos {
  private AdminDtos() {
  }

  public record AdminSummaryDto(
      int totalUsers,
      int totalServices,
      int totalStores,
      int totalSchedules,
      int totalOrders,
      int pendingOrders,
      int confirmedOrders,
      int completedOrders,
      int totalNegatives,
      int visibleNegatives,
      int paidRevenueCent) {
  }

  public record AdminServiceDto(
      String id,
      String categoryId,
      String categoryName,
      String name,
      String coverUrl,
      int priceCent,
      int durationMin,
      String description,
      String tagsJson,
      BigDecimal rating,
      boolean enabled,
      String createdAt) {
  }

  public record AdminStoreDto(
      String id,
      String name,
      String address,
      BigDecimal distanceKm,
      BigDecimal rating,
      int reviews,
      String hours,
      String tagsJson,
      String coverUrl,
      boolean enabled,
      String createdAt) {
  }

  public record AdminScheduleDto(
      String id,
      String storeId,
      String storeName,
      String serviceId,
      String serviceName,
      String serviceDate,
      String startTime,
      String endTime,
      int capacity,
      int bookedCount,
      String status,
      String createdAt) {
  }

  public record AdminOrderDto(
      String id,
      String orderNo,
      String userId,
      String userPhone,
      String serviceId,
      String serviceName,
      String storeId,
      String storeName,
      String scheduleId,
      String contactName,
      String contactPhone,
      int priceCent,
      String appointmentAt,
      String status,
      String payStatus,
      boolean userHidden,
      String createdAt) {
  }

  public record AdminNegativeDto(
      String id,
      String userId,
      String userPhone,
      String orderId,
      String orderNo,
      String title,
      String type,
      String imageUrl,
      String fileId,
      String downloadUrl,
      String status,
      String createdAt) {
  }

  public record AdminAuditLogDto(
      String id,
      String action,
      String targetType,
      String targetId,
      String detailJson,
      String createdAt) {
  }

  public record UpsertServiceRequest(
      Long categoryId,
      String name,
      String coverUrl,
      Integer priceCent,
      Integer durationMin,
      String description,
      String tagsJson,
      BigDecimal rating,
      Boolean enabled) {
  }

  public record UpsertStoreRequest(
      String name,
      String address,
      BigDecimal distanceKm,
      BigDecimal rating,
      Integer reviews,
      String hours,
      String tagsJson,
      String coverUrl,
      Boolean enabled) {
  }

  public record UpsertScheduleRequest(
      Long storeId,
      Long serviceId,
      String serviceDate,
      String startTime,
      String endTime,
      Integer capacity,
      String status) {
  }

  public record UpdateOrderRequest(String status, String payStatus, Boolean userHidden) {
  }
}
