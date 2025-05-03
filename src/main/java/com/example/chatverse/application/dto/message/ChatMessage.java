package com.example.chatverse.application.dto.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a message transferred via Kafka. (Data Transfer Object for Kafka messages)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Builder.Default
    private String messageId = UUID.randomUUID().toString();

    private String senderId;
    private String recipientId;
    private String roomId;
    private String content;

    @Builder.Default
    private Instant timestamp = Instant.now();
}