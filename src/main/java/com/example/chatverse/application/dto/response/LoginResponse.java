package com.example.chatverse.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    @Schema(description = "Refresh токен пользователя")
    private String refreshToken;
    @Schema(description = "Access токен пользователя")
    private String accessToken;
    @Schema(description = "ID пользователя")
    private Long userId;
    @Schema(description = "Флаг, указывающий, существует ли пользователь")
    private boolean isUserExists;
}
