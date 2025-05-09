package com.example.chatverse.application.dto.contact;

import com.example.chatverse.domain.entity.ContactStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class UpdateContactStatusRequestDto {
    /**
     * Новый желаемый статус. Ожидается ACCEPTED или DECLINED.
     */
    @NotNull(message = "New status cannot be null")
    private ContactStatus newStatus;
}
    