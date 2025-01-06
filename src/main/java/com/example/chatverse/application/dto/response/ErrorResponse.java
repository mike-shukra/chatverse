package com.example.chatverse.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Формат ответа в случае ошибки")
public class ErrorResponse {
    @Schema(description = "Метка времени ошибки", example = "2024-12-30T10:00:00")
    private String timestamp;

    @Schema(description = "HTTP-статус ошибки", example = "400")
    private int status;

    @Schema(description = "Краткое описание ошибки", example = "Bad Request")
    private String error;

    @Schema(description = "Сообщение об ошибке", example = "Phone number is required")
    private String message;

    @Schema(description = "Путь запроса, где произошла ошибка", example = "/api/v1/users/register")
    private String path;
}
