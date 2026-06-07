package com.amberfilm.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAdminNegativeRequest(
    @NotNull Long orderId,
    @NotBlank String title,
    @NotBlank @Pattern(regexp = "^(original|retouched)$", message = "必须是 original 或 retouched") String type,
    Long fileId,
    String imageUrl,
    String status) {
}
