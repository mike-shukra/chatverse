package com.example.chatverse.application.dto.contact;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendContactRequestDto {
    @NotNull(message = "Target user ID cannot be null")
    private Long targetUserId;
}
    