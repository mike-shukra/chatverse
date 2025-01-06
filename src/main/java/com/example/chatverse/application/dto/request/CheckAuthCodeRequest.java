package com.example.chatverse.application.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckAuthCodeRequest {
    @NotBlank
    @Schema(description = "Номер телефона пользователя")
    private String phone;
    @NotBlank
    @Schema(description = "Код аутентификации")
    private String code;
}