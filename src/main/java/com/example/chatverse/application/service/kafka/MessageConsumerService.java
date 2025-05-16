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

    // Простая вспомогательная функция для сокращения длинных сообщений в логах
    private String getContentSnippet(String content, int maxLength) {
        if (content == null) {
            return "null";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength - 3) + "...";
    }

    @KafkaListener(topics = "${app.kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listenChatMessages(
            @Payload ChatMessage incomingMessageDto,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received Kafka message: ID=[{}], Key=[{}], Partition=[{}], Offset=[{}], Topic=[{}], Payload=[{}]",
                incomingMessageDto.getMessageId(), key, partition, offset, topic, incomingMessageDto);

        try {
            ChatMessageEntity messageEntity = chatMessageMapper.toEntity(incomingMessageDto);
            ChatMessageEntity savedEntity = chatMessageRepository.save(messageEntity);
            log.info("Message ID=[{}] (DB ID: [{}]) saved to database. Saved entity: {}",
                    incomingMessageDto.getMessageId(), savedEntity.getId(), savedEntity);

            ChatMessage messageToSendViaWebSocket = chatMessageMapper.toDto(savedEntity);
            String contentSnippet = getContentSnippet(messageToSendViaWebSocket.getContent(), 50); // Ограничим до 50 символов

            if (messageToSendViaWebSocket.getRoomId() != null) {
                String roomTopic = "/topic/messages/" + messageToSendViaWebSocket.getRoomId();
                messagingTemplate.convertAndSend(roomTopic, messageToSendViaWebSocket);
                // Усиленное логирование для отправки в комнату
                log.info("Sent to room topic: MessageID=[{}], RoomTopic=[{}], SenderID=[{}], RecipientID=[{}], ContentSnippet='{}'",
                        messageToSendViaWebSocket.getMessageId(),
                        roomTopic,
                        messageToSendViaWebSocket.getSenderId(),
                        messageToSendViaWebSocket.getRecipientId(), // Может быть null для комнат, но полезно видеть
                        contentSnippet);
            } else {
                log.warn("Message ID=[{}] has no roomId. Cannot send to WebSocket room topic.", messageToSendViaWebSocket.getMessageId());
            }

            if (messageToSendViaWebSocket.getRecipientId() != null) {
                String recipientUser = messageToSendViaWebSocket.getRecipientId().toString();
                String privateQueueSuffix = "/queue/messages";
                messagingTemplate.convertAndSendToUser(recipientUser, privateQueueSuffix, messageToSendViaWebSocket);
                // Усиленное логирование для персональной отправки
                log.info("Sent private notification: MessageID=[{}], RecipientUserID=[{}], Destination=[/user/{}{}], SenderID=[{}], ContentSnippet='{}'",
                        messageToSendViaWebSocket.getMessageId(),
                        recipientUser,
                        recipientUser, // для формирования полного пути в логе
                        privateQueueSuffix,
                        messageToSendViaWebSocket.getSenderId(),
                        contentSnippet);
            }

            // Для еще более детального логирования, можно добавить на уровне DEBUG:
            // log.debug("Full message DTO sent via WebSocket: {}", messageToSendViaWebSocket);

        } catch (Exception e) {
            log.error("Error processing received Kafka message: ID=[{}], Key=[{}], Error: {}",
                    incomingMessageDto.getMessageId(), key, e.getMessage(), e);
            throw e;
        }
    }
}