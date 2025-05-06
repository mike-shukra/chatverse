package com.example.chatverse.application.dto.message;

import lombok.Data;

@Data
public class SendMessageRequestDto {
    private Long recipientId; // Обязательно для приватного сообщения
    private String content;
    // Можно добавить другие поля, если нужны для создания сообщения, но не roomId
}
