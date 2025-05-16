package com.example.chatverse.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Имя пользователя")
    private String name;

    @Schema(description = "Username пользователя")
    private String username;

    @Schema(description = "Дата рождения пользователя", example = "1990-01-01")
    private String birthday;

    @Schema(description = "Город проживания пользователя")
    private String city;

    @Schema(description = "Ссылка на профиль VK пользователя")
    private String vk;

    @Schema(description = "Ссылка на профиль Instagram пользователя")
    private String instagram;

    @Schema(description = "Статус пользователя")
    private String status;

    @Schema(description = "URL аватара пользователя")
    private String avatar;

    @Schema(description = "ID пользователя")
    private Long id;

    @Schema(description = "Последний визит пользователя", example = "2024-12-30T10:00:00")
    private String last;

    @Schema(description = "Флаг, указывающий на онлайн-статус пользователя")
    private boolean online;

    @Schema(description = "Дата создания профиля пользователя", example = "2024-12-30T10:00:00")
    private String created;

    @Schema(description = "Номер телефона пользователя")
    private String phone;

    @Schema(description = "Аватары пользователя в различных размерах")
    private Avatars avatars;
}
