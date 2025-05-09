package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.response.LoginResponse;
import com.example.chatverse.application.dto.response.TokenResponse;
import com.example.chatverse.application.mapper.UserMapper;
import com.example.chatverse.domain.entity.PlatformUser;
import com.example.chatverse.domain.entity.RefreshToken;
import com.example.chatverse.domain.repository.RefreshTokenRepository;
import com.example.chatverse.domain.repository.UserRepository;
import com.example.chatverse.infrastructure.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15; // 15 минут
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 дней
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    /**
     *TODO Хранилище для кодов авторизации (заглушка)
     */
    private final Map<String, String> authCodes = new HashMap<>();

    public AuthService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Отправка кода авторизации на указанный телефон
     */
    public boolean sendAuthCode(String phone) {
        String authCode = String.format("%04d", new Random().nextInt(10000));

        //TODO Логика отправки SMS (заглушка)
        log.info("Sending auth code: {} to phone: {}", authCode, phone);

        authCodes.put(phone, authCode);

        return true;
    }

    /**
     * Проверка кода авторизации
     */
    public LoginResponse checkAuthCode(String phone, String code) {

        if (!authCodes.containsKey(phone)) {
            throw new IllegalArgumentException("No auth code sent for this phone");
        }

        String expectedCode = authCodes.get(phone);
        if (!expectedCode.equals(code)) {
            throw new IllegalArgumentException("Invalid auth code");
        }

        authCodes.remove(phone);
        boolean isUserExists = userRepository.existsByPhone(phone);
        PlatformUser user;

        if (isUserExists) {
            user = userRepository.findByPhone(phone).get();
        } else {
            user = UserMapper.phoneToEntity(phone);
            System.out.println("checkAuthCode user: " + user);
            user = userRepository.save(user);
            System.out.println("checkAuthCode user save: " + user);
        }
        String accessToken = generateToken(user.getId().toString(), ACCESS_TOKEN_EXPIRATION);
        System.out.println("accessToken: " + accessToken);
        String refreshToken = generateRefreshToken(user.getId());
        System.out.println("refreshToken" + refreshToken);

        return LoginResponse.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .userId(user.getId())
                .isUserExists(isUserExists)
                .build();
    }

    /**
     * Генерация токенов для пользователя
     */
    public TokenResponse generateTokens(Long userId) {
        String accessToken = generateToken(userId.toString(), ACCESS_TOKEN_EXPIRATION);
        String refreshToken = generateRefreshToken(userId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    /**
     * Обновление токенов по refresh токену
     */
    public TokenResponse refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(token);
            throw new IllegalArgumentException("Refresh token expired");
        }

        return generateTokens(refreshToken.getUserId());
    }

    /**
     * Генерация нового refresh токена
     */
    private String generateRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRATION / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    /**
     * Генерация JWT
     */
    private String generateToken(String userId, long expirationTime) {
        try {
            System.out.println("generateToken userId: " + userId + " expirationTime: " + expirationTime);
            return jwtUtils.generateToken(userId, expirationTime);
        } catch (Exception e) {
            System.err.println("Error generating token: " + e.getMessage());
            throw new RuntimeException("Error generating token", e);
        }
    }

    /**
     * Проверка JWT
     */
    public boolean validateToken(String token) {
        try {
            return jwtUtils.validateToken(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }

    public boolean checkJwt(String token) {
        try {
            return validateToken(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }
    }
}
