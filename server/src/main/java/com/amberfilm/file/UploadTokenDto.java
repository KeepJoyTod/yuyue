package com.amberfilm.file;

public record UploadTokenDto(
    String fileId,
    String storageProvider,
    String bucket,
    String objectKey,
    String uploadUrl,
    String assetUrl,
    String method,
    String expiresAt) {
}
