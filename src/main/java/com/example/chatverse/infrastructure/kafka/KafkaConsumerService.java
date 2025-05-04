package com.example.chatverse.infrastructure.kafka;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.mapper.ChatMessageMapper;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import com.example.chatverse.domain.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final SimpMessagingTemplate messagingTemplate; // Для отправки сообщений через WebSocket
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    // Убедитесь, что groupId совпадает с тем, что в логах (chatverse-app-group)
    @KafkaListener(topics = "chat-messages", groupId = "chatverse-app-group")
    @Transactional // Управляем транзакцией для сохранения в БД
    public void consume(ChatMessage message) {
        log.info("Received message from Kafka: {}", message);

        try {
            // 1. Сохраняем сообщение в базу данных
            ChatMessageEntity entity = chatMessageMapper.toEntity(message);
            chatMessageRepository.save(entity);
            log.info("Message saved to DB with ID: {}", entity.getId());

            // 2. Отправляем сообщение подписчикам WebSocket
            // Путь должен соответствовать тому, на что подписывается клиент
            String destination = "/topic/room/" + message.getRoomId();
            messagingTemplate.convertAndSend(destination, message);
            log.info("Message sent to WebSocket destination: {}", destination);

        } catch (Exception e) {
            // Важно обрабатывать ошибки, чтобы consumer не падал
            log.error("Error processing Kafka message: {}", message, e);
            // Здесь можно добавить логику повторной обработки или отправки в DLQ (Dead Letter Queue)
        }
    }
}