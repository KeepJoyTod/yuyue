package com.amberfilm.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneLoginRequest(
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "必须是 11 位手机号") String phone,
    @NotBlank String code) {
}

