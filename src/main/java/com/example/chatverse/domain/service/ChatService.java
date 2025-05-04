package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.mapper.ChatMessageMapper;
// Импортируем MessageProducerService
import com.example.chatverse.application.service.kafka.MessageProducerService;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import com.example.chatverse.domain.repository.ChatMessageRepository;
import com.example.chatverse.domain.repository.UserRepository;
import com.example.chatverse.infrastructure.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Удаляем импорт KafkaTemplate
// import org.springframework.kafka.core.KafkaTemplate;
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
     * Подготавливает и отправляет сообщение через MessageProducerService.
     * @param message DTO сообщения (ожидается recipientId и content).
     * @param authentication Информация об аутентифицированном пользователе.
     */
    public void sendMessage(ChatMessage message, Authentication authentication) {
        Long senderId;
        Object principal = authentication.getPrincipal();
        // --- Логика извлечения senderId остается без изменений ---
        if (principal instanceof UserDetails userDetails) {
            try {
                senderId = Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.error("Could not parse user ID from username: {}", userDetails.getUsername(), e);
                throw new IllegalArgumentException("Invalid user ID format in authentication principal.");
            }
        } else if (principal instanceof String username) {
            try {
                senderId = Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.error("Could not parse user ID from principal string: {}", username, e);
                throw new IllegalArgumentException("Invalid user ID format in authentication principal.");
            }
        }
        else {
            log.error("Unexpected principal type: {}", principal.getClass().getName());
            throw new IllegalArgumentException("Cannot determine sender ID from authentication principal.");
        }
        // --- Конец логики извлечения senderId ---


        // Проверяем существование получателя
        Long recipientId = message.getRecipientId();
        if (recipientId == null) {
            // Если это не групповой чат (roomId не задан явно клиентом), то получатель обязателен
            if (message.getRoomId() == null) {
                log.error("Recipient ID is null and Room ID is null for message content: {}", message.getContent());
                throw new IllegalArgumentException("Recipient ID cannot be null for a private message.");
            }
            // Если roomId задан, то recipientId может быть null (например, сообщение в общую комнату)
            // Но в нашей текущей логике roomId генерируется на основе sender/recipient,
            // поэтому для 1-на-1 чатов recipientId должен быть.
            // Если планируются групповые чаты с явным roomId, эту логику нужно будет пересмотреть.
        } else if (!userRepository.existsById(recipientId)) {
            throw new UserNotFoundException("Recipient user with ID " + recipientId + " not found.");
        }

        // Генерируем ID комнаты (для 1-на-1 чата), если он не задан
        String roomId = message.getRoomId();
        if (roomId == null && recipientId != null) {
            roomId = generateRoomId(senderId, recipientId);
        } else if (roomId == null) {
            // Ситуация, когда нет ни recipientId, ни roomId - не должна происходить при текущей логике
            log.error("Cannot determine roomId: recipientId is null and roomId is null.");
            throw new IllegalArgumentException("Cannot determine target for the message (missing recipientId or roomId).");
        }


        // Заполняем DTO перед отправкой
        message.setSenderId(senderId);
        message.setRoomId(roomId); // Устанавливаем сгенерированный или исходный roomId
        message.setTimestamp(Instant.now());
        message.setMessageId(UUID.randomUUID().toString()); // Генерируем уникальный ID

        log.info("Prepared message, sending via MessageProducerService: {}", message);
        messageProducerService.sendMessage(message);
        // Сохранение в БД теперь происходит в MessageConsumerService после получения из Kafka
    }

    /**
     * Получает историю сообщений для комнаты.
     * @param roomId ID комнаты чата.
     * @param authentication Информация об аутентифицированном пользователе.
     * @return Список DTO сообщений.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessageHistory(String roomId, Authentication authentication) {
        // TODO: Добавить проверку, имеет ли текущий пользователь доступ к этой комнате
        // Например, извлечь ID пользователей из roomId и сравнить с ID из authentication
        // Long currentUserId = ... получить ID из authentication ...
        // String[] userIds = roomId.split("_");
        // if (!userIds[0].equals(String.valueOf(currentUserId)) && !userIds[1].equals(String.valueOf(currentUserId))) {
        //    throw new AccessDeniedException("User does not have access to this chat room.");
        // }

        List<ChatMessageEntity> messageEntities = chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        log.info("Retrieved {} messages for room ID '{}'", messageEntities.size(), roomId);
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
        List<Long> ids = Arrays.asList(userId1, userId2);
        ids.sort(Comparator.naturalOrder());
        return ids.stream().map(String::valueOf).collect(Collectors.joining("_"));
    }
}
