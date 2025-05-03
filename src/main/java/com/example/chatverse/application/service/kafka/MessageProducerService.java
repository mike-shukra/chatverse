package com.example.chatverse.application.service.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending messages to Kafka.
 */
@Service
public class MessageProducerService {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerService.class);

    @Value("${app.kafka.topic.chat-messages:chat-messages}")
    private String chatTopicName;

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Autowired
    public MessageProducerService(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a chat message to the configured Kafka topic.
     * Uses roomId or recipientId as the Kafka message key for partitioning.
     *
     * @param message The ChatMessage object to send.
     */
    public void sendMessage(ChatMessage message) {
        if (message == null || chatTopicName == null) {
            log.error("Cannot send null message or topic name is not configured.");
            return;
        }

        String key = message.getRoomId() != null ? message.getRoomId() : message.getRecipientId();
        if (key == null) {
            key = message.getSenderId();
            log.warn("Message key is null (no roomId or recipientId), using senderId: {}", key);
        }

        log.debug("Attempting to send message with key [{}]: {}", key, message);

        CompletableFuture<SendResult<String, ChatMessage>> future = kafkaTemplate.send(chatTopicName, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}] to topic=[{}] partition=[{}]",
                        message.getMessageId(),
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("Unable to send message=[{}] due to : {}", message.getMessageId(), ex.getMessage(), ex);
                // Consider adding error handling logic (retry, DLQ, etc.)
            }
        });
    }
}