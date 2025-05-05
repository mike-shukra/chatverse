package com.example.chatverse.application.dto.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a message transferred via Kafka and WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    private Long senderId;
    private Long recipientId;
    private String roomId;
    private String content;

    @Builder.Default
    private Instant timestamp = Instant.now();
}