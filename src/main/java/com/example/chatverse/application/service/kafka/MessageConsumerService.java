package com.example.chatverse.application.service.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.mapper.ChatMessageMapper;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import com.example.chatverse.domain.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component responsible for consuming messages from Kafka topics.
 */
@Component
@RequiredArgsConstructor
public class MessageConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    // TODO: Inject your WebSocket handling service here
    // private final WebSocketService webSocketService;

    @KafkaListener(topics = "${app.kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listenChatMessages(
            @Payload ChatMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received message: ID=[{}], Key=[{}], Partition=[{}], Offset=[{}], Topic=[{}], Payload=[{}]",
                message.getMessageId(), key, partition, offset, topic, message);

        try {
            // 1. Преобразуем DTO в Entity
            ChatMessageEntity messageEntity = chatMessageMapper.toEntity(message);
            // Убедись, что маппер корректно преобразует DTO в Entity

            // 2. Сохраняем сущность и получаем сохраненную версию
            ChatMessageEntity savedEntity = chatMessageRepository.save(messageEntity);

            // 3. Выводим в лог информацию о сохраненной сущности
            // Используем ID из сохраненной сущности, т.к. он мог быть сгенерирован БД
            // Предполагается, что у ChatMessageEntity есть адекватный метод toString() (например, через Lombok @Data или @ToString)
            log.info("Message ID=[{}] saved to database. Saved entity: {}", savedEntity.getMessageId(), savedEntity);

            // 4. Отправляем сообщение через WebSocket (когда будет реализовано)
            if (savedEntity.getRecipientId() != null && savedEntity.getRoomId() != null) { // Используем данные из savedEntity
                log.debug("Processing private message for recipient: {} in room {}", savedEntity.getRecipientId(), savedEntity.getRoomId());
                // webSocketService.sendMessageToUser(savedEntity.getRecipientId(), chatMessageMapper.toDto(savedEntity)); // Отправляем DTO
            } else if (savedEntity.getRoomId() != null) {
                log.debug("Processing room message for room: {}", savedEntity.getRoomId());
                // webSocketService.sendMessageToRoom(savedEntity.getRoomId(), chatMessageMapper.toDto(savedEntity)); // Отправляем DTO
            } else {
                log.warn("Saved message without recipientId or roomId: {}", savedEntity.getMessageId());
            }

        } catch (Exception e) {
            log.error("Error processing received message: ID=[{}], Key=[{}], Error: {}",
                    message.getMessageId(), key, e.getMessage(), e);
            throw e; // Перебрасываем исключение для отката транзакции и/или повторной обработки
        }
    }
}