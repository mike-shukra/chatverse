package com.example.chatverse.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
public class TokenResponse {
    @Schema(description = "Refresh токен пользователя")
    private String refreshToken;

    @Schema(description = "Access токен пользователя")
    private String accessToken;

    @Schema(description = "ID пользователя")
    private Long userId;
}
