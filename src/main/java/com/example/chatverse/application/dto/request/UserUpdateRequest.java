package com.example.chatverse.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank
    @Schema(description = "Имя пользователя")
    private String name;
    @NotBlank
    @Schema(description = "Username пользователя")
    private String username;
    @Schema(description = "Дата рождения пользователя")
    private String birthday;
    @Schema(description = "Город проживания пользователя")
    private String city;
    @Schema(description = "Ссылка на профиль VK пользователя")
    private String vk;
    @Schema(description = "Ссылка на профиль Instagram пользователя")
    private String instagram;
    @Schema(description = "Статус пользователя")
    private String status;
    @Schema(description = "Аватар пользователя")
    private UploadImage avatar;
}