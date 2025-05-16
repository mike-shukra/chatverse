package com.example.chatverse.infrastructure.configuration;

import com.example.chatverse.infrastructure.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.security.core.Authentication;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем эндпоинт /ws, к которому будут подключаться клиенты
        // withSockJS() обеспечивает fallback для браузеров без нативной поддержки WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns( // Укажи здесь домены твоего фронтенда
                        "http://localhost:8888", // Локальный фронтенд
                        "http://chatverse.local:8888", // Локальный фронтенд через host
                        "http://localhost:30080", // Kubernetes NodePort
                        "http://chatverse.local:30080", // Kubernetes NodePort через host
                        "http://chatverse.local",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1",
                        "http://localhost:5173",
                        "http://localhost"
                )
                .withSockJS()
                .setHeartbeatTime(25000); // 25 секунд
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


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                    // Также можно проверить 'X-Authorization', если используется как альтернатива
                    // String xAuthorizationHeader = accessor.getFirstNativeHeader("X-Authorization");

                    String token = null;
                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        token = authorizationHeader.substring(7);
                    }
                    // else if (xAuthorizationHeader != null && xAuthorizationHeader.startsWith("Bearer ")) {
                    //     token = xAuthorizationHeader.substring(7);
                    // }


                    if (token != null) {
                        try {
                            String userId = jwtUtils.extractUserId(token); // Или extractUsername, если у тебя так
                            // jwtUtils.isTokenValid(token, userDetails) // Полная валидация, если нужна
                            // Для STOMP обычно достаточно проверки подписи и срока действия,
                            // так как пользователь уже прошел HTTP аутентификацию.

                            // Устанавливаем Principal для WebSocket сессии
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userId, // principal - обычно ID пользователя или username
                                    null,   // credentials
                                    Collections.emptyList() // authorities
                            );
                            accessor.setUser(authentication);
                            System.out.println("STOMP CONNECT: Authenticated user " + userId + " for session " + accessor.getSessionId());
                        } catch (Exception e) {
                            System.err.println("STOMP CONNECT: Invalid JWT token in STOMP header. " + e.getMessage());
                            // Можно здесь выбросить исключение, чтобы прервать соединение,
                            // или вернуть null/сообщение об ошибке, чтобы клиент получил ERROR фрейм.
                            // Например, throw new MessagingException("Invalid token");
                            // Это приведет к закрытию WebSocket соединения.
                            return null; // Или обработать ошибку более явно
                        }
                    } else {
                        System.err.println("STOMP CONNECT: No token found in STOMP header for session " + accessor.getSessionId());
                        // Можно также прервать соединение, если токен обязателен
                        // return null;
                    }
                }
                return message;
            }
        });
    }
}