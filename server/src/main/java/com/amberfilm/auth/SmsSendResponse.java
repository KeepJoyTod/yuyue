package com.amberfilm.auth;

public record SmsSendResponse(String phone, String scene, String expiresAt, String devCode) {
}
