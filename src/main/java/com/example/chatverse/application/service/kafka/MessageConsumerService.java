package com.example.chatverse.application.service.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.mapper.ChatMessageMapper; // Добавить импорт
import com.example.chatverse.domain.entity.ChatMessageEntity; // Добавить импорт
import com.example.chatverse.domain.repository.ChatMessageRepository; // Добавить импорт
import lombok.RequiredArgsConstructor; // Добавить для конструктора
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Для атомарности (опционально, но желательно)

/**
 * Component responsible for consuming messages from Kafka topics.
 */
@Component
@RequiredArgsConstructor // Используем Lombok для внедрения зависимостей
public class MessageConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);

    // Внедряем зависимости через конструктор (благодаря @RequiredArgsConstructor)
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    // TODO: Inject your WebSocket handling service here
    // private final WebSocketService webSocketService;

    @KafkaListener(topics = "${app.kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional // Опционально: чтобы сохранение в БД и коммит офсета были атомарны
    public void listenChatMessages(
            @Payload ChatMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key // Key might be null
    ) {
        log.info("Received message: ID=[{}], Key=[{}], Partition=[{}], Offset=[{}], Topic=[{}], Payload=[{}]",
                message.getMessageId(), key, partition, offset, topic, message);

        try {
            // --- НАЧАЛО ЛОГИКИ ОБРАБОТКИ ---

            // 1. Сохраняем сообщение в базу данных
            ChatMessageEntity messageEntity = chatMessageMapper.toEntity(message);
            // Важно: Убедись, что маппер корректно преобразует DTO в Entity
            // Возможно, нужно дозаполнить какие-то поля Entity, если их нет в DTO
            chatMessageRepository.save(messageEntity);
            log.info("Message ID=[{}] saved to database.", message.getMessageId());

            // 2. Отправляем сообщение через WebSocket (когда будет реализовано)
            if (message.getRecipientId() != null && message.getRoomId() != null) { // Убедимся что roomId есть для приватных чатов
                log.debug("Processing private message for recipient: {} in room {}", message.getRecipientId(), message.getRoomId());
                // webSocketService.sendMessageToUser(message.getRecipientId(), message); // Раскомментировать когда будет WebSocketService
                // Возможно, нужно отправлять и отправителю для отображения в его окне
                // webSocketService.sendMessageToUser(message.getSenderId(), message);
                // Или лучше отправлять в комнату, а фронтенд разберется
                // webSocketService.sendMessageToRoom(message.getRoomId(), message);
            } else if (message.getRoomId() != null) { // Для общих комнат (если будут)
                log.debug("Processing room message for room: {}", message.getRoomId());
                // webSocketService.sendMessageToRoom(message.getRoomId(), message);
            } else {
                log.warn("Received message without recipientId or roomId: {}", message.getMessageId());
                // Возможно, такие сообщения не должны сохраняться или требуют особой обработки
            }
            // --- КОНЕЦ ЛОГИКИ ОБРАБОТКИ ---

            // Если не было исключений, Spring Kafka автоматически коммитит offset (если enable.auto.commit=false и нет @Transactional)
            // Если используется @Transactional, коммит произойдет после успешного завершения транзакции.

        } catch (Exception e) {
            log.error("Error processing received message: ID=[{}], Key=[{}], Error: {}",
                    message.getMessageId(), key, e.getMessage(), e);
            // Важно: Если произошла ошибка (например, при сохранении в БД),
            // offset НЕ должен быть закоммичен, чтобы сообщение было обработано повторно.
            // Spring Kafka с @Transactional или без auto-commit позаботится об этом.
            // Рассмотри настройку Dead Letter Queue (DLQ) для сообщений, которые не удается обработать после нескольких попыток.
            throw e; // Перебрасываем исключение, чтобы Spring Kafka понял, что обработка не удалась
        }
    }
}