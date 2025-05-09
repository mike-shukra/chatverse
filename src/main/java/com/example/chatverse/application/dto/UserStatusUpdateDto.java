package com.example.chatverse.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusUpdateDto {
    private Long userId;
    private boolean online;
    private LocalDateTime timestamp; // Время последнего изменения статуса или lastSeen
}