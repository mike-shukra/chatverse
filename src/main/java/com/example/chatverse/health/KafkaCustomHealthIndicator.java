package com.example.chatverse.health; // Используй имя своего пакета

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
// Можно добавить логгер, если нужно
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Component("kafka") // Регистрируем как бин и даем имя "kafka" для эндпоинта /actuator/health
public class KafkaCustomHealthIndicator implements HealthIndicator {

    // private static final Logger log = LoggerFactory.getLogger(KafkaCustomHealthIndicator.class);

    private final KafkaAdmin kafkaAdmin;
    private final long requestTimeoutMs = 5000; // Таймаут для запроса к Kafka (5 секунд)

    // Внедряем KafkaAdmin через конструктор
    public KafkaCustomHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        // Используем try-with-resources, чтобы AdminClient гарантированно закрылся
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {

            // Устанавливаем опции для запроса, включая таймаут
            DescribeClusterOptions options = new DescribeClusterOptions().timeoutMs((int) requestTimeoutMs);

            // Отправляем запрос на получение информации о кластере
            DescribeClusterResult result = client.describeCluster(options);

            // Пытаемся получить ID кластера, ожидая не дольше таймаута
            String clusterId = result.clusterId().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            // Можно также получить количество узлов для дополнительной информации
            int nodeCount = result.nodes().get(requestTimeoutMs, TimeUnit.MILLISECONDS).size();

            // Проверяем, что ID кластера получен
            if (clusterId != null && !clusterId.isEmpty()) {
                // Если все хорошо, возвращаем статус UP с деталями
                return Health.up()
                        .withDetail("clusterId", clusterId)
                        .withDetail("nodes", nodeCount)
                        .build();
            } else {
                // Если ID не пришел (маловероятно при успешном запросе, но на всякий случай)
                return Health.down().withDetail("error", "Cluster ID not available").build();
            }

        } catch (Exception e) {
            // Если произошла любая ошибка (таймаут, недоступность Kafka и т.д.)
            // log.warn("Kafka health check failed: {}", e.getMessage()); // Раскомментируй для логирования
            // Возвращаем статус DOWN и добавляем исключение в детали
            // (будет видно в /actuator/health, если включено show-details=always)
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}