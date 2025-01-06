package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.response.LoginResponse;
import com.example.chatverse.application.dto.response.TokenResponse;
import com.example.chatverse.application.mapper.UserMapper;
import com.example.chatverse.domain.entity.PlatformUser;
import com.example.chatverse.domain.entity.RefreshToken;
import com.example.chatverse.domain.repository.RefreshTokenRepository;
import com.example.chatverse.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AuthService {

    private static final String SECRET_KEY = "your-secret-key";
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15; // 15 минут
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 дней

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     *TODO Хранилище для кодов авторизации (заглушка)
     */
    private final Map<String, String> authCodes = new HashMap<>();

    public AuthService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Отправка кода авторизации на указанный телефон
     */
    public boolean sendAuthCode(String phone) {
        String authCode = String.format("%04d", new Random().nextInt(10000));

        //TODO Логика отправки SMS (заглушка)
        System.out.println("Sending auth code: " + authCode + " to phone: " + phone);
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
            user = userRepository.save(user);
        }
        String accessToken = generateToken(user.getId().toString(), ACCESS_TOKEN_EXPIRATION);
        String refreshToken = generateRefreshToken(user.getId());

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
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    /**
     * Проверка JWT
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }

    public void checkJwt(String token) {
        try {
            // Проверка валидности токена
            Claims claims = validateToken(token);

            // Дополнительная логика: проверка срока действия токена, ролей и т.д.
            System.out.println("Token is valid. User ID: " + claims.getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }
    }
}
