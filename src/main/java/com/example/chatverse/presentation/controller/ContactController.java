package com.example.chatverse.presentation.controller;

import com.example.chatverse.application.dto.contact.PendingRequestResponseDto;
import com.example.chatverse.application.dto.contact.SendContactRequestDto;
import com.example.chatverse.application.dto.contact.ContactResponseDto;
import com.example.chatverse.application.dto.contact.UpdateContactStatusRequestDto;
import com.example.chatverse.domain.entity.ContactStatus;
import com.example.chatverse.domain.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Contacts", description = "API для управления контактами пользователей")
@SecurityRequirement(name = "bearer-key")
@PreAuthorize("isAuthenticated()") // Все методы требуют аутентификации
public class ContactController {

    private final ContactService contactService;

    private Long getCurrentUserId(Authentication authentication) {
        // Предполагаем, что имя пользователя в Authentication - это его ID
        return Long.parseLong(authentication.getName());
    }

    @Operation(summary = "Отправить запрос на добавление в контакты")
    @ApiResponse(responseCode = "201", description = "Запрос успешно отправлен")
    @PostMapping("/requests")
    public ResponseEntity<Void> sendContactRequest(
            @Valid @RequestBody SendContactRequestDto requestDto,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        contactService.sendContactRequest(currentUserId, requestDto.getTargetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Принять или отклонить входящий запрос на добавление в контакты")
    @ApiResponse(responseCode = "200", description = "Статус запроса успешно обновлен")
    @PutMapping("/requests/{requesterId}")
    public ResponseEntity<Void> updateContactRequestStatus(
            @PathVariable @Parameter(description = "ID пользователя, отправившего запрос") Long requesterId,
            @Valid @RequestBody UpdateContactStatusRequestDto statusDto,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        contactService.updateContactRequestStatus(currentUserId, requesterId, statusDto.getNewStatus());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Удалить пользователя из списка контактов")
    @ApiResponse(responseCode = "204", description = "Контакт успешно удален")
    @DeleteMapping("/{contactUserId}")
    public ResponseEntity<Void> removeContact(
            @PathVariable @Parameter(description = "ID пользователя, которого нужно удалить из контактов") Long contactUserId,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        contactService.removeContact(currentUserId, contactUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить список своих контактов")
    @ApiResponse(responseCode = "200", description = "Список контактов успешно получен")
    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> getContacts(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        List<ContactResponseDto> contacts = contactService.getContacts(currentUserId);
        return ResponseEntity.ok(contacts);
    }

    @Operation(summary = "Получить список ожидающих запросов на добавление в контакты")
    @ApiResponse(responseCode = "200", description = "Список запросов успешно получен")
    @GetMapping("/requests/pending")
    public ResponseEntity<List<PendingRequestResponseDto>> getPendingRequests(
            @RequestParam(name = "direction", defaultValue = "INCOMING") @Parameter(description = "Тип запросов: INCOMING или OUTGOING") String direction,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean incoming = "INCOMING".equalsIgnoreCase(direction);
        List<PendingRequestResponseDto> requests = contactService.getPendingRequests(currentUserId, incoming);
        return ResponseEntity.ok(requests);
    }

    // TODO: Эндпоинты для блокировки/разблокировки, поиска пользователей
}