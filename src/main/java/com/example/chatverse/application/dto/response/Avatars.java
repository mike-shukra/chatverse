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
@AllArgsConstructor
@NoArgsConstructor
public class Avatars implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "URL основного аватара")
    private String avatar;
    @Schema(description = "URL большого аватара")
    private String bigAvatar;
    @Schema(description = "URL миниатюры аватара")
    private String miniAvatar;
}
