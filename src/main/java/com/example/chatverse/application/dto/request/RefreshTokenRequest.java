package com.example.chatverse.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank
    @Schema(description = "Refresh токен для обновления access токена")
    private String refreshToken;
}
