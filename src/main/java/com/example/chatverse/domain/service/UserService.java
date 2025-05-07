package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.request.RegisterIn;
import com.example.chatverse.application.dto.request.UserUpdateRequest;
import com.example.chatverse.application.dto.response.Avatars;
import com.example.chatverse.application.dto.response.TokenResponse;
import com.example.chatverse.application.dto.response.UserProfileResponse;
import com.example.chatverse.application.dto.response.UserUpdateResponse;
import com.example.chatverse.application.mapper.UserMapper;
import com.example.chatverse.domain.entity.PlatformUser;
import com.example.chatverse.domain.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Регистрация нового пользователя
     */
    public TokenResponse registerUser(RegisterIn request) {
        // Проверка существования пользователя с таким телефоном или именем пользователя
        if (userRepository.findByPhone(request.getPhone()).isPresent() ||
                userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User with given phone or username already exists");
        }

        // Создание нового пользователя
        PlatformUser user = PlatformUser.builder()
                .phone(request.getPhone())
                .username(request.getUsername())
                .name(request.getName())
                .created(LocalDateTime.now())
                .online(false)
                .completedTask(0)
                .build();

        userRepository.save(user);

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
        // Заглушка: проверка кода (реальная проверка должна быть на стороне SMS API)
        if (!"1234".equals(code)) {
            throw new IllegalArgumentException("Invalid authorization code");
        }

        // Поиск пользователя по телефону
        PlatformUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        user.setOnline(true);
        userRepository.save(user);

        // Возвращаем токены
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
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Создание объекта Avatars (заглушка)
        Avatars avatars = Avatars.builder()
                .avatar("default-avatar.png")
                .bigAvatar("default-big-avatar.png")
                .miniAvatar("default-mini-avatar.png")
                .build();

        // Создание объекта UserProfileResponse
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
                .avatars(avatars) // Устанавливаем объект Avatars
                .build();
    }
    /**
     * Обновление профиля пользователя
     */
    @CacheEvict(value = "users", key = "#userId")
    public UserUpdateResponse updateUserProfile(Long userId, UserUpdateRequest request) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user = UserMapper.updateEntityFromRequest(request, user);
        userRepository.save(user);

        Avatars avatars = Avatars.builder()
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
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(userId);
    }

    /**
     * Проверка активности пользователя
     */
    public boolean isUserOnline(Long userId) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.isOnline();
    }

    /**
     * Выход пользователя (установка оффлайн)
     */
    @CacheEvict(value = "users", key = "#userId")
    public void logoutUser(Long userId) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setOnline(false);
        userRepository.save(user);
    }
}
