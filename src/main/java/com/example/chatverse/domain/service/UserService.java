package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.UserStatusUpdateDto; // Импортируем наш DTO
import com.example.chatverse.application.dto.request.RegisterIn;
import com.example.chatverse.application.dto.request.UserUpdateRequest;
import com.example.chatverse.application.dto.response.Avatars;
import com.example.chatverse.application.dto.response.TokenResponse;
import com.example.chatverse.application.dto.response.UserProfileResponse;
import com.example.chatverse.application.dto.response.UserUpdateResponse;
import com.example.chatverse.application.mapper.UserMapper;
import com.example.chatverse.domain.entity.PlatformUser;
import com.example.chatverse.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j; // Добавляем для логирования
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Импортируем SimpMessagingTemplate
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@Slf4j // Включаем логирование
public class UserService {
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // Добавляем зависимость

    // Обновляем конструктор для инъекции SimpMessagingTemplate
    public UserService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Регистрация нового пользователя
     */
    public TokenResponse registerUser(RegisterIn request) {
        if (userRepository.findByPhone(request.getPhone()).isPresent() ||
                userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User with given phone or username already exists");
        }

        PlatformUser user = PlatformUser.builder()
                .phone(request.getPhone())
                .username(request.getUsername())
                .name(request.getName())
                .created(LocalDateTime.now())
                .online(false) // Изначально оффлайн
                .completedTask(0)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());

        // Возвращаем токены (заглушка)
        return TokenResponse.builder()
                .refreshToken("mock-refresh-token")
                .accessToken("mock-access-token")
                .userId(user.getId())
                .build();
    }

    /**
     * Авторизация пользователя по телефону и проверке кода
     */
    public TokenResponse authorizeUser(String phone, String code) {
        if (!"1234".equals(code)) { // Заглушка: реальная проверка кода
            throw new IllegalArgumentException("Invalid authorization code");
        }

        PlatformUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found with phone: " + phone));

        user.setLastLogin(LocalDateTime.now());
        user.setOnline(true);
        userRepository.save(user);
        log.info("User authorized and set online: {}", user.getUsername());

        // Оповещение через WebSocket об изменении статуса
        UserStatusUpdateDto statusUpdate = new UserStatusUpdateDto(user.getId(), true, user.getLastLogin());
        messagingTemplate.convertAndSend("/topic/user.status", statusUpdate);
        log.info("Sent online status update via WebSocket for user: {}", user.getUsername());

        return TokenResponse.builder()
                .refreshToken("mock-refresh-token")
                .accessToken("mock-access-token")
                .userId(user.getId())
                .build();
    }

    /**
     * Получение профиля текущего пользователя
     */
    @Cacheable(value = "users", key = "#userId")
    public UserProfileResponse getCurrentUser(Long userId) {
        log.debug("Fetching user profile for userId: {}", userId);
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Avatars avatars = Avatars.builder()
                .avatar("default-avatar.png")
                .bigAvatar("default-big-avatar.png")
                .miniAvatar("default-mini-avatar.png")
                .build();

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .birthday(user.getBirthday() != null ? user.getBirthday().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                .city(user.getCity())
                .vk(user.getVk())
                .instagram(user.getInstagram())
                .status(user.getStatus())
                .phone(user.getPhone())
                .last(user.getLastLogin() != null ? user.getLastLogin().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .online(user.isOnline())
                .created(user.getCreated() != null ? user.getCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .avatars(avatars)
                .build();
    }
    /**
     * Обновление профиля пользователя
     */
    @CacheEvict(value = "users", key = "#userId") // Можно использовать @CachePut, если хотим обновить кэш сразу
    public UserUpdateResponse updateUserProfile(Long userId, UserUpdateRequest request) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user = UserMapper.updateEntityFromRequest(request, user); // Предполагается, что UserMapper существует и работает
        userRepository.save(user);
        log.info("User profile updated for userId: {}", userId);

        // Здесь можно было бы обновить и статус, если он меняется, и отправить WebSocket уведомление
        // Например, если бы в UserUpdateRequest было поле online

        Avatars avatars = Avatars.builder() // Заглушка для аватаров
                .avatar("updated-avatar.png")
                .bigAvatar("updated-big-avatar.png")
                .miniAvatar("updated-mini-avatar.png")
                .build();

        return new UserUpdateResponse(avatars);
    }

    /**
     * Удаление пользователя
     */
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("User deleted with ID: {}", userId);
        // Здесь можно было бы отправить уведомление о том, что пользователь удален, если это нужно
        // Например, UserStatusUpdateDto с каким-то специальным флагом или просто null
    }

    /**
     * Проверка активности пользователя
     * Этот метод может быть не так актуален, если статусы рассылаются через WebSocket,
     * но может быть полезен для первоначальной загрузки или редких проверок.
     */
    public boolean isUserOnline(Long userId) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return user.isOnline();
    }

    /**
     * Выход пользователя (установка оффлайн)
     */
    @CacheEvict(value = "users", key = "#userId")
    public void logoutUser(Long userId) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setOnline(false);
        // Можно также обновлять user.setLastLogin(LocalDateTime.now()); если это время последнего выхода
        userRepository.save(user);
        log.info("User logged out and set offline: {}", user.getUsername());

        // Оповещение через WebSocket об изменении статуса
        UserStatusUpdateDto statusUpdate = new UserStatusUpdateDto(user.getId(), false, LocalDateTime.now()); // Время выхода
        messagingTemplate.convertAndSend("/topic/user.status", statusUpdate);
        log.info("Sent offline status update via WebSocket for user: {}", user.getUsername());
    }
}