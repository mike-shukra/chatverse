package com.example.chatverse.application.dto.contact;

import com.example.chatverse.domain.entity.ContactStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PendingRequestResponseDto {
    private Long contactEntityId; // ID самой записи Contact
    private Long otherUserId;
    private String otherUserUsername;
    private String otherUserName;
    // private String otherUserAvatarUrl;
    private ContactStatus requestStatus; // Должен быть PENDING
    private String direction; // "INCOMING" или "OUTGOING"
    private LocalDateTime requestTimestamp;
}
    