package com.example.chatverse.application.dto.contact;

import com.example.chatverse.domain.entity.ContactStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContactResponseDto {
    private Long userId;
    private String username;
    private String name;
    // private String avatarUrl; // Раскомментируйте, если у PlatformUser есть поле аватара
    private boolean online;
    private LocalDateTime lastSeen;
    private ContactStatus friendshipStatus; // Статус дружбы (ACCEPTED)
    private LocalDateTime becameContactsAt; // Когда стали контактами
}
    