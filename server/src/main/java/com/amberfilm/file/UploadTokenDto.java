package com.amberfilm.file;

public record UploadTokenDto(
    String storageProvider,
    String objectKey,
    String uploadUrl,
    String assetUrl,
    String method,
    String expiresAt) {
}
