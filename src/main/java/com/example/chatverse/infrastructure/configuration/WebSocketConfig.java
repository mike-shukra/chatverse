package com.example.chatverse.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Включает обработку WebSocket сообщений через брокер
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем эндпоинт /ws, к которому будут подключаться клиенты
        // withSockJS() обеспечивает fallback для браузеров без нативной поддержки WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOrigins( // Укажи здесь домены твоего фронтенда
                        "http://localhost:8888", // Локальный фронтенд
                        "http://chatverse.local:8888", // Локальный фронтенд через host
                        "http://localhost:30080", // Kubernetes NodePort
                        "http://chatverse.local:30080", // Kubernetes NodePort через host
                        "http://chatverse.local"  // Если доступ через ingress без порта
                        // Добавь другие необходимые origins
                )
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Настраивает брокер сообщений.
        // /topic - префикс для сообщений, которые рассылаются всем подписчикам (публичные чаты, комнаты).
        // /queue - префикс для сообщений, предназначенных конкретному пользователю (личные сообщения).
        registry.enableSimpleBroker("/topic", "/queue");

        // /app - префикс для сообщений, которые должны быть обработаны методами с аннотацией @MessageMapping.
        // Пока мы не используем @MessageMapping напрямую для отправки, но префикс лучше задать.
        registry.setApplicationDestinationPrefixes("/app");

        // Можно настроить префикс для сообщений конкретному пользователю, если понадобится
         registry.setUserDestinationPrefix("/user");
    }
}