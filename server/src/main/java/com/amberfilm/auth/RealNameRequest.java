package com.amberfilm.auth;

import jakarta.validation.constraints.NotBlank;

public record RealNameRequest(@NotBlank String realName, String idCardNo) {
}

