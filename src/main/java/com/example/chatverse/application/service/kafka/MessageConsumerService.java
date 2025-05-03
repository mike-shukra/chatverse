package com.example.chatverse.application.service.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Component responsible for consuming messages from Kafka topics.
 */
@Component
public class MessageConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);

    // TODO: Inject your WebSocket handling service here
    // private final WebSocketService webSocketService;
    //
    // @Autowired
    // public MessageConsumerService(WebSocketService webSocketService) {
    //     this.webSocketService = webSocketService;
    // }

    @KafkaListener(topics = "${app.kafka.topic.chat-messages:chat-messages}",
            groupId = "${spring.kafka.consumer.group-id}")
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
            // --- YOUR MESSAGE PROCESSING LOGIC HERE ---
            if (message.getRecipientId() != null) {
                log.debug("Processing private message for recipient: {}", message.getRecipientId());
                // webSocketService.sendMessageToUser(message.getRecipientId(), message);
            } else if (message.getRoomId() != null) {
                log.debug("Processing room message for room: {}", message.getRoomId());
                // webSocketService.sendMessageToRoom(message.getRoomId(), message);
            } else {
                log.warn("Received message without recipientId or roomId: {}", message.getMessageId());
            }
            // --- END OF YOUR LOGIC ---

        } catch (Exception e) {
            log.error("Error processing received message: ID=[{}], Key=[{}], Error: {}",
                    message.getMessageId(), key, e.getMessage(), e);
            // Decide how to handle processing errors (retry, DLQ, etc.)
        }
    }
}