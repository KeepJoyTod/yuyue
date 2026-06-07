package com.amberfilm.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateBookingRequest(
    @NotNull Long serviceId,
    @NotNull Long storeId,
    @NotNull Long scheduleId,
    @NotBlank String contactName,
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "必须是 11 位手机号") String contactPhone) {
}

