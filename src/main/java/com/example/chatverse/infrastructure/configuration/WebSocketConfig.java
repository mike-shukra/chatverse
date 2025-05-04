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
        // Регистрирует эндпоинт /ws, который клиенты будут использовать для подключения.
        // withSockJS() обеспечивает фолбэк для браузеров без поддержки WebSocket.
        // setAllowedOrigins("*") разрешает подключения с любых доменов (для разработки).
        // В продакшене укажите конкретные разрешенные домены!
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost", "http://localhost:8888", "http://chatverse.local:8888", "http://localhost:30080", "http://chatverse.local:30080", "http://chatverse.local") // Используем разрешенные из SecurityConfig
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
        // registry.setUserDestinationPrefix("/user");
    }
}