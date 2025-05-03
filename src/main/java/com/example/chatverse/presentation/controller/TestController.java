package com.example.chatverse.presentation.controller;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.service.kafka.MessageProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Test Controller", description = "Эндпоинты для тестирования интеграций (Redis, Kafka)")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class); // Добавляем логгер

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private MessageProducerService messageProducerService;

    @Operation(summary = "Тестирование Redis", description = "Записывает и читает тестовое значение из Redis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная запись и чтение из Redis / RedisTemplate недоступен / Ошибка при работе с Redis")
    })
    @GetMapping("/test/redis")
    public String testRedis() {
        if (redisTemplate == null) {
            log.warn("RedisTemplate is not available.");
            return "RedisTemplate не доступен.";
        }
        try {
            String key = "test-key";
            String value = "Hello Redis at " + Instant.now();
            redisTemplate.opsForValue().set(key, value);
            String retrievedValue = redisTemplate.opsForValue().get(key);
            log.info("Redis test successful. Written: '{}', Retrieved: '{}'", value, retrievedValue);
            return "Записано в Redis: " + value + "<br>Прочитано из Redis: " + retrievedValue;
        } catch (Exception e) {
            log.error("Ошибка при работе с Redis: {}", e.getMessage(), e); // Используем логгер
            return "Ошибка при работе с Redis: " + e.getMessage();
        }
    }

    // --- Старый метод для отправки строк (можно оставить для сравнения или удалить) ---
    @Operation(summary = "Тестирование Kafka (отправка строки)", description = "Отправляет тестовую строку в указанный топик Kafka (используя старый подход).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено / Ошибка при отправке в Kafka")
    })
    @GetMapping("/test/kafka/string") // Изменил путь, чтобы не конфликтовать
    public String testKafkaString(
            @Parameter(description = "Имя топика Kafka", example = "test-topic")
            @RequestParam(defaultValue = "test-topic") String topic,
            @Parameter(description = "Текст сообщения", example = "Hello Kafka!")
            @RequestParam(defaultValue = "Hello Kafka!") String message) {
        // Этот метод теперь может не работать, если KafkaTemplate<String, String> не сконфигурирован
        // или можно временно внедрить его снова для теста
        log.warn("Attempting to use direct KafkaTemplate<String, String> which might not be configured correctly anymore.");
        return "This endpoint might be deprecated or require specific KafkaTemplate<String, String> configuration.";
    }

    // --- Новый метод для отправки ChatMessage ---
    @Operation(summary = "Тестирование Kafka (отправка ChatMessage)", description = "Создает и отправляет тестовый объект ChatMessage в топик 'chat-messages' через MessageProducerService.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение ChatMessage успешно отправлено / MessageProducerService недоступен / Ошибка при отправке"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса")
    })
    @PostMapping("/test/kafka/chatmessage") // Используем POST, т.к. создаем ресурс (сообщение)
    public String testKafkaChatMessage(
            @Parameter(description = "ID отправителя", example = "user123") @RequestParam String senderId,
            @Parameter(description = "ID получателя (для личного сообщения)", example = "user456") @RequestParam(required = false) String recipientId,
            @Parameter(description = "ID комнаты (для группового чата)", example = "room789") @RequestParam(required = false) String roomId,
            @Parameter(description = "Текст сообщения", example = "Hello from TestController!") @RequestParam(defaultValue = "Test message content") String content
    ) {
        if (messageProducerService == null) {
            log.warn("MessageProducerService is not available.");
            return "MessageProducerService не доступен.";
        }
        if (recipientId == null && roomId == null) {
            return "Ошибка: Укажите либо recipientId, либо roomId.";
        }
        if (recipientId != null && roomId != null) {
            return "Ошибка: Укажите ТОЛЬКО recipientId ИЛИ roomId, не оба.";
        }

        try {
            // Создаем объект ChatMessage с помощью билдера
            ChatMessage testMessage = ChatMessage.builder()
                    .senderId(senderId)
                    .recipientId(recipientId) // Будет null, если не указан в запросе
                    .roomId(roomId)           // Будет null, если не указан в запросе
                    .content(content + " at " + Instant.now())
                    // messageId и timestamp генерируются по умолчанию в билдере
                    .build();

            // Отправляем сообщение через наш сервис
            messageProducerService.sendMessage(testMessage);

            log.info("ChatMessage sent via MessageProducerService: {}", testMessage);
            return "Сообщение ChatMessage отправлено: " + testMessage.toString();

        } catch (Exception e) {
            log.error("Ошибка при отправке ChatMessage в Kafka: {}", e.getMessage(), e);
            return "Ошибка при отправке ChatMessage в Kafka: " + e.getMessage();
        }
    }

    // --- Альтернативный метод для отправки ChatMessage через тело запроса ---
    @Operation(summary = "Тестирование Kafka (отправка ChatMessage из JSON)", description = "Отправляет объект ChatMessage, полученный в теле POST-запроса (JSON), в топик 'chat-messages'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сообщение ChatMessage успешно отправлено / MessageProducerService недоступен / Ошибка при отправке"),
            @ApiResponse(responseCode = "400", description = "Некорректный JSON в теле запроса")
    })
    @PostMapping("/test/kafka/chatmessage/json")
    public String testKafkaChatMessageJson(@RequestBody ChatMessage message) {
        if (messageProducerService == null) {
            log.warn("MessageProducerService is not available.");
            return "MessageProducerService не доступен.";
        }
        // Можно добавить валидацию полученного объекта message
        if (message.getSenderId() == null || message.getContent() == null || (message.getRecipientId() == null && message.getRoomId() == null)) {
            return "Ошибка: Некорректные данные в JSON. Требуются senderId, content и (recipientId или roomId).";
        }

        try {
            // Дополняем сообщение актуальным временем, если нужно (или используем то, что пришло)
            // message.setTimestamp(Instant.now()); // Раскомментировать, если нужно перезаписать время

            messageProducerService.sendMessage(message);
            log.info("ChatMessage from JSON body sent via MessageProducerService: {}", message);
            return "Сообщение ChatMessage из JSON отправлено: " + message.toString();
        } catch (Exception e) {
            log.error("Ошибка при отправке ChatMessage (из JSON) в Kafka: {}", e.getMessage(), e);
            return "Ошибка при отправке ChatMessage (из JSON) в Kafka: " + e.getMessage();
        }
    }
}