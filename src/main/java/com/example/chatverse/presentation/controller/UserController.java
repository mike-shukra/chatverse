package com.example.chatverse.presentation.controller;

import com.example.chatverse.application.dto.request.*;
import com.example.chatverse.application.dto.response.*;
import com.example.chatverse.domain.service.AuthService;
import com.example.chatverse.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @Autowired
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Operation(summary = "Регистрация пользователя", description = "Создаёт нового пользователя и возвращает токены.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован.",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Validated @RequestBody RegisterIn request) {
        TokenResponse tokenResponse = userService.registerUser(request);
        return ResponseEntity.status(201).body(tokenResponse);
    }

    @Operation(summary = "Получение текущего пользователя", description = "Возвращает профиль текущего пользователя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль успешно получен.",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@RequestParam Long userId) {
        UserProfileResponse profile = userService.getCurrentUser(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Обновление профиля пользователя", description = "Обновляет данные профиля пользователя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль успешно обновлён.",
                    content = @Content(schema = @Schema(implementation = UserUpdateResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<UserUpdateResponse> updateUser(@RequestParam Long userId, @Validated @RequestBody UserUpdateRequest request) {
        UserUpdateResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удаление пользователя", description = "Удаляет пользователя по ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удалён."),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отправка кода авторизации", description = "Отправляет код авторизации на указанный номер телефона.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Код успешно отправлен.",
                    content = @Content(schema = @Schema(implementation = SuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный номер телефона.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/send-auth-code")
    public ResponseEntity<SuccessResponse> sendAuthCode(@Validated @RequestBody SendAuthCodeRequest request) {
        boolean isSuccess = authService.sendAuthCode(request.getPhone());
        return ResponseEntity.status(201).body(new SuccessResponse(isSuccess));
    }

    @Operation(summary = "Проверка кода авторизации", description = "Проверяет код авторизации и возвращает токены.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Код успешно проверен.",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный код авторизации.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/check-auth-code")
    public ResponseEntity<LoginResponse> checkAuthCode(@Validated @RequestBody CheckAuthCodeRequest request) {
        LoginResponse response = authService.checkAuthCode(request.getPhone(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Проверка JWT", description = "Проверяет валидность JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен валиден."),
            @ApiResponse(responseCode = "401", description = "Токен не валиден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-jwt")
    public ResponseEntity<Void> checkJwt(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is missing or invalid");
        }

        String token = authHeader.substring(7); // Удаление "Bearer " из начала строки
        authService.checkJwt(token);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Обновление токена", description = "Обновляет access и refresh токены.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены.",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный refresh токен.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@Validated @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Выход пользователя", description = "Устанавливает пользователя в статус offline.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь успешно вышел."),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(@RequestParam Long userId) {
        userService.logoutUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Проверка активности пользователя", description = "Проверяет, находится ли пользователь в статусе online.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Проверка успешна.",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/online")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable Long userId) {
        boolean isOnline = userService.isUserOnline(userId);
        return ResponseEntity.ok(isOnline);
    }
}
