package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.dto.message.SendMessageRequestDto;
import com.example.chatverse.application.mapper.ChatMessageMapper;
import com.example.chatverse.application.service.kafka.MessageProducerService;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import com.example.chatverse.domain.repository.ChatMessageRepository;
import com.example.chatverse.domain.repository.UserRepository;
import com.example.chatverse.infrastructure.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageProducerService messageProducerService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * Извлекает ID отправителя из объекта Authentication.
     * @param authentication Информация об аутентифицированном пользователе.
     * @return ID отправителя.
     * @throws IllegalArgumentException если ID пользователя не может быть определен.
     */
    private Long extractSenderIdFromAuth(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.error("Could not parse user ID from username: {}", userDetails.getUsername(), e);
                throw new IllegalArgumentException("Invalid user ID format in authentication principal (UserDetails).");
            }
        } else if (principal instanceof String username) {
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.error("Could not parse user ID from principal string: {}", username, e);
                throw new IllegalArgumentException("Invalid user ID format in authentication principal (String).");
            }
        } else {
            log.error("Unexpected principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new IllegalArgumentException("Cannot determine sender ID from authentication principal.");
        }
    }

    /**
     * Подготавливает и отправляет сообщение через MessageProducerService.
     * @param requestDto DTO с данными для нового сообщения (ожидается recipientId и content).
     * @param authentication Информация об аутентифицированном пользователе.
     */
    public void sendMessage(SendMessageRequestDto requestDto, Authentication authentication) {
        Long senderId = extractSenderIdFromAuth(authentication);

        Long recipientId = requestDto.getRecipientId();
        if (recipientId == null) {
            log.error("Recipient ID is null for message content: {}", requestDto.getContent());
            throw new IllegalArgumentException("Recipient ID cannot be null for a private message.");
        }
        if (!userRepository.existsById(recipientId)) {
            throw new UserNotFoundException("Recipient user with ID " + recipientId + " not found.");
        }

        // Генерируем ID комнаты
        String roomId = generateRoomId(senderId, recipientId);

        // Создаем полное ChatMessage DTO для отправки в Kafka
        ChatMessage kafkaMessage = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString()) // Генерируем уникальный ID
                .senderId(senderId)
                .recipientId(recipientId) // recipientId здесь нужен для информации и для WebSocket, если он будет его использовать
                .roomId(roomId)           // Устанавливаем сгенерированный roomId
                .content(requestDto.getContent())
                .timestamp(Instant.now())
                .build();

        log.info("Prepared message, sending via MessageProducerService: {}", kafkaMessage);
        messageProducerService.sendMessage(kafkaMessage); // Отправляем полное DTO
    }

    /**
     * Получает историю сообщений для комнаты.
     * @param roomId ID комнаты чата.
     * @param authentication Информация об аутентифицированном пользователе.
     * @return Список DTO сообщений.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessageHistory(String roomId, Authentication authentication) {
        Long currentUserId = extractSenderIdFromAuth(authentication);

        // Проверка доступа к комнате
        String[] userIdsInRoom = roomId.split("_");
        if (userIdsInRoom.length != 2) { // Базовая проверка формата roomId
            log.warn("Invalid roomId format encountered: {}", roomId);
            throw new AccessDeniedException("Invalid room ID format.");
        }

        boolean userIsInRoom = Arrays.stream(userIdsInRoom)
                .anyMatch(idStr -> idStr.equals(String.valueOf(currentUserId)));

        if (!userIsInRoom) {
            log.warn("User {} attempted to access room {} without permission.", currentUserId, roomId);
            throw new AccessDeniedException("User does not have access to this chat room.");
        }

        List<ChatMessageEntity> messageEntities = chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        log.info("Retrieved {} messages for room ID '{}' for user {}", messageEntities.size(), roomId, currentUserId);
        return chatMessageMapper.toDtoList(messageEntities);
    }

    /**
     * Генерирует уникальный и консистентный ID комнаты для двух пользователей.
     * @param userId1 ID первого пользователя.
     * @param userId2 ID второго пользователя.
     * @return Строковый ID комнаты.
     */
    private String generateRoomId(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            log.error("Cannot generate room ID with null user IDs: userId1={}, userId2={}", userId1, userId2);
            throw new IllegalArgumentException("User IDs cannot be null for generating a room ID.");
        }
        // Предотвращаем создание комнаты с самим собой, если это не предполагается бизнес-логикой
        if (userId1.equals(userId2)) {
            log.warn("Attempted to generate a room ID for a user with themselves: userId={}", userId1);
            // Можно либо бросить исключение, либо вернуть специальный ID, либо разрешить,
            // в зависимости от требований. Пока бросим исключение для ясности.
            throw new IllegalArgumentException("Cannot generate a room ID for a user with themselves.");
        }

        List<Long> ids = Arrays.asList(userId1, userId2);
        ids.sort(Comparator.naturalOrder());
        return ids.stream().map(String::valueOf).collect(Collectors.joining("_"));
    }
}