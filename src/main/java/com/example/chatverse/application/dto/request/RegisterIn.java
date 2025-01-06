package com.example.chatverse.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterIn {
    @NotBlank(message = "Phone number is required")
    private String phone;
    @NotBlank(message = "Username is required")
    private String username;
    @NotBlank(message = "Name is required")
    private String name;
}
