package com.example.chatverse.presentation.controller;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.dto.message.SendMessageRequestDto;
import com.example.chatverse.domain.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessageViaWebSocket(
            @Payload SendMessageRequestDto requestDto,
            Principal principal
    ) {

        if (principal == null) {
            log.error("Principal is null for WebSocket message. Request: {}", requestDto);
            return;
        }

        if (requestDto.getRecipientId() == null || requestDto.getContent() == null || requestDto.getContent().trim().isEmpty()) {
            log.error("Invalid SendMessageRequestDto received via WebSocket. Principal: {}, Request: {}", principal.getName(), requestDto);
            return;
        }

        String principalName = principal.getName();
        Long senderId;
        try {
            senderId = Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            log.error("Could not parse senderId from principal name '{}' for WebSocket message. Request: {}",
                    principalName, requestDto, e);
            return;
        }

        Long recipientId = requestDto.getRecipientId();
        String roomId;
        try {
            roomId = chatService.generateRoomId(senderId, recipientId);
        } catch (IllegalArgumentException e) {
            log.error("Error generating roomId for senderId {} and recipientId {}: {}", senderId, recipientId, e.getMessage());
            // Можно отправить ошибку клиенту, если это необходимо
            // messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Error processing recipient: " + e.getMessage());
            return;
        }


        ChatMessage fullChatMessage = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .senderId(senderId)
                .recipientId(recipientId)
                .roomId(roomId)
                .content(requestDto.getContent())
                .timestamp(Instant.now())
                .build();

        log.info("Processing WebSocket message via SendMessageRequestDto: ID={}, Room={}, Sender={}, Recipient={}, Content='{}'",
                fullChatMessage.getMessageId(),
                fullChatMessage.getRoomId(),
                fullChatMessage.getSenderId(),
                fullChatMessage.getRecipientId(),
                fullChatMessage.getContent()
        );

        try {
            chatService.sendWebSocketChatMessage(fullChatMessage);
        } catch (Exception e) {
            log.error("Error calling ChatService to send WebSocket message: {}", fullChatMessage, e);
            // messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Failed to send message");
            return;
        }
    }
}