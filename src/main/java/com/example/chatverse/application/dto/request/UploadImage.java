package com.example.chatverse.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UploadImage {
    @Schema(description = "Имя файла изображения")
    private String filename;
    @Schema(description = "Base64-кодированное содержимое изображения")
    private String base_64;
}
