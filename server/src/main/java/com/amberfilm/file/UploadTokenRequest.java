package com.amberfilm.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UploadTokenRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @NotNull @Positive Long sizeByte,
    @NotBlank String usage) {
}
