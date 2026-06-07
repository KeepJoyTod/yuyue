package com.amberfilm.auth;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(@NotBlank String code, String phone) {
}
