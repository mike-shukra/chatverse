package com.example.chatverse.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId; // UUID сообщения, совпадает с DTO

    @Column(name = "sender_id", nullable = false)
    private Long senderId; // ID пользователя-отправителя

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId; // ID пользователя-получателя (для личных сообщений)

    @Column(name = "room_id", nullable = false)
    private String roomId; // Идентификатор комнаты чата

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // Текст сообщения

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp; // Время отправки
}