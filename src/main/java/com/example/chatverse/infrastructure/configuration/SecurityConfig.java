package com.example.chatverse.infrastructure.configuration;

import com.example.chatverse.application.dto.response.ErrorResponse;
import com.example.chatverse.infrastructure.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays; // <<< Импортируем Arrays
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Предполагаем, что JwtAuthenticationFilter принимает JwtUtils в конструкторе
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Применяем конфигурацию CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Отключаем CSRF, так как используем JWT и REST API без сессий
                .csrf(AbstractHttpConfigurer::disable)
                // Настраиваем правила авторизации запросов
                .authorizeHttpRequests(auth -> auth
                        // <<< Явно разрешаем OPTIONS запросы для всех путей
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Разрешаем доступ без аутентификации к публичным эндпоинтам
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/users/send-auth-code",
                                "/api/v1/users/check-auth-code",
                                "/api/v1/users/refresh-token",
                                "/actuator/health",
                                "/actuator/info",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers("/test/**").permitAll() //TODO
                        // Запрещаем доступ ко всем остальным actuator эндпоинтам (можно настроить для роли ADMIN позже)
                        .requestMatchers("/actuator/**").denyAll() //TODO: Настроить доступ для ADMIN
                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                // Устанавливаем политику управления сессиями на STATELESS (без сессий)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Настраиваем обработчики исключений безопасности
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(customAccessDeniedHandler()) // Обработчик для ошибки 403 Forbidden
                        .authenticationEntryPoint(customAuthenticationEntryPoint()) // Обработчик для ошибки 401 Unauthorized
                )
                // Добавляем наш JWT фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    private AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access Denied: {} {}", request.getMethod(), request.getRequestURI(), accessDeniedException); // Логгируем ошибку
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .error("Forbidden")
                    .message("Access Denied")
                    .path(request.getRequestURI())
                    .build();

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try {
                // Используем инжектированный objectMapper
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } catch (IOException e) {
                log.error("Error writing Access Denied response", e);
            }
        };
    }

    private AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized access attempt: {} {}", request.getMethod(), request.getRequestURI(), authException); // Логгируем ошибку
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .error("Unauthorized")
                    .message("Authentication required")
                    .path(request.getRequestURI())
                    .build();
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                // Используем инжектированный objectMapper
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } catch (IOException e) {
                log.error("Error writing Authentication Entry Point response", e);
            }
        };
    }

    // Этот метод больше не нужен, так как objectMapper инжектируется
    // private String convertToJson(ErrorResponse errorResponse) throws IOException {
    //     return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(errorResponse);
    // }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // ВАЖНО: Для отладки используем "*", но для продакшена укажи конкретные домены!
        configuration.setAllowedOrigins(List.of(
                "http://localhost",
                "http://localhost:8888",
                "http://chatverse.local:8888",
                "http://localhost:30080",
                "http://chatverse.local:30080",
                "http://chatverse.local",
                "http://127.0.0.1:5173",
                "http://127.0.0.1"
                // "*" // Можно временно использовать для отладки, если список выше не помогает
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Добавляем PATCH, если нужно
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")); // Добавляем стандартные заголовки
        // configuration.setAllowCredentials(true); // Раскомментируй, если используешь cookies или Authorization
        configuration.setMaxAge(3600L); // Кэширование preflight запросов на 1 час

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Применяем конфигурацию ко всем путям
        source.registerCorsConfiguration("/**", configuration);
        log.info("CORS configuration applied with allowed origins: {}", configuration.getAllowedOrigins()); // Логгируем конфигурацию CORS
        return source;
    }
}