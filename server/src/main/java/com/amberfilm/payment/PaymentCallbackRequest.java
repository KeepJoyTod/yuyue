package com.amberfilm.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCallbackRequest(
    @NotBlank String orderNo,
    @NotBlank String transactionNo,
    @NotNull @Positive Integer amountCent,
    String signature,
    String rawPayload) {
}
