package com.example.chatverse.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuccessResponse {
    @Schema(description = "Флаг, указывающий на успешность операции")
    private boolean isSuccess;
}