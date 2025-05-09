package com.example.chatverse.application.service.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.mapper.ChatMessageMapper;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import com.example.chatverse.domain.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component responsible for consuming messages from Kafka topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageConsumerService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;


    @KafkaListener(topics = "${app.kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional // Важно для атомарности сохранения в БД и последующих действий
    public void listenChatMessages(
            @Payload ChatMessage incomingMessageDto, // Сообщение уже десериализовано Kafka в DTO
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received Kafka message: ID=[{}], Key=[{}], Partition=[{}], Offset=[{}], Topic=[{}], Payload=[{}]",
                incomingMessageDto.getMessageId(), key, partition, offset, topic, incomingMessageDto);

        try {
            // 1. Преобразуем DTO в Entity
            ChatMessageEntity messageEntity = chatMessageMapper.toEntity(incomingMessageDto);

            // 2. Сохраняем сущность и получаем сохраненную версию (может содержать сгенерированные БД значения, например, ID)
            ChatMessageEntity savedEntity = chatMessageRepository.save(messageEntity);
            log.info("Message ID=[{}] (DB ID: [{}]) saved to database. Saved entity: {}",
                    incomingMessageDto.getMessageId(), savedEntity.getId(), savedEntity);

            // 3. Преобразуем сохраненную сущность обратно в DTO для отправки через WebSocket
            // Это гарантирует, что мы отправляем самые актуальные данные, включая те, что могли быть изменены/добавлены при сохранении
            ChatMessage messageToSendViaWebSocket = chatMessageMapper.toDto(savedEntity);

            // 4. Отправляем сообщение в общий топик комнаты через WebSocket
            if (messageToSendViaWebSocket.getRoomId() != null) {
                String roomTopic = "/topic/messages/" + messageToSendViaWebSocket.getRoomId();
                messagingTemplate.convertAndSend(roomTopic, messageToSendViaWebSocket);
                log.info("Message ID=[{}] sent to WebSocket room topic: {}", messageToSendViaWebSocket.getMessageId(), roomTopic);
            } else {
                // Этого быть не должно, если логика формирования roomId корректна перед отправкой в Kafka
                log.warn("Message ID=[{}] has no roomId. Cannot send to WebSocket room topic.", messageToSendViaWebSocket.getMessageId());
            }

            // 5. Отправляем персональное уведомление получателю, если он указан
            // Это полезно для нотификаций пользователя о новом сообщении в любом из его чатов
            if (messageToSendViaWebSocket.getRecipientId() != null) {
                String recipientUser = messageToSendViaWebSocket.getRecipientId().toString();
                String privateQueue = "/queue/private"; // Spring автоматически преобразует в /user/{username}/queue/private
                messagingTemplate.convertAndSendToUser(recipientUser, privateQueue, messageToSendViaWebSocket);
                log.info("Message ID=[{}] sent as private notification to user: {} on destination {}",
                        messageToSendViaWebSocket.getMessageId(), recipientUser, privateQueue);
            }

        } catch (Exception e) {
            log.error("Error processing received Kafka message: ID=[{}], Key=[{}], Error: {}",
                    incomingMessageDto.getMessageId(), key, e.getMessage(), e);
            // Перебрасываем исключение. Это приведет к откату транзакции.
            // Kafka listener обработает это в соответствии со своей конфигурацией (например, повторная попытка или отправка в DLQ).
            throw e;
        }
    }
}