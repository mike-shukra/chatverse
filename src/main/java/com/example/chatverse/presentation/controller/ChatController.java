package com.example.chatverse.presentation.controller;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.application.dto.message.SendMessageRequestDto;
import com.example.chatverse.domain.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "API для управления чат-сообщениями")
@SecurityRequirement(name = "bearer-key") // Требуем аутентификацию для всех методов контроллера
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Отправить сообщение", description = "Отправляет сообщение указанному получателю.")
    @ApiResponse(responseCode = "200", description = "Сообщение успешно отправлено в очередь.")
    @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (например, не указан получатель или контент).")
    @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован.")
    @ApiResponse(responseCode = "404", description = "Получатель не найден.")
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()") // Только аутентифицированные пользователи
    public ResponseEntity<Void> sendMessage(
            @Validated @RequestBody @Parameter(description = "Данные сообщения (требуются recipientId и content)") SendMessageRequestDto requestDto,
            Authentication authentication) {
        chatService.sendMessage(requestDto, authentication); // Передаем новое DTO в сервис
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить историю сообщений", description = "Возвращает историю сообщений для указанной комнаты чата.")
    @ApiResponse(responseCode = "200", description = "История сообщений успешно получена.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatMessage.class)))
    @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован.")
    @ApiResponse(responseCode = "403", description = "У пользователя нет доступа к этой комнате.") // TODO: Реализовать проверку доступа
    @GetMapping("/messages/{roomId}")
    @PreAuthorize("isAuthenticated()") // Только аутентифицированные пользователи
    public ResponseEntity<List<ChatMessage>> getMessageHistory(
            @PathVariable @Parameter(description = "ID комнаты чата (например, '1_2')") String roomId,
            Authentication authentication) {
        List<ChatMessage> history = chatService.getMessageHistory(roomId, authentication);
        return ResponseEntity.ok(history);
    }
}