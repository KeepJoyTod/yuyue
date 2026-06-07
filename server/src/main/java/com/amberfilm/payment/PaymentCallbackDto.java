package com.amberfilm.payment;

public record PaymentCallbackDto(String orderId, String orderNo, String status, boolean duplicate) {
}
