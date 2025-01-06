package com.example.chatverse.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendAuthCodeRequest {
    @NotBlank
    @Size(max = 30)
    @Schema(description = "Номер телефона для отправки кода")
    private String phone;
}
