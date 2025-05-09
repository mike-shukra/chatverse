package com.example.chatverse.application.mapper;

import com.example.chatverse.application.dto.contact.ContactResponseDto;
import com.example.chatverse.application.dto.contact.PendingRequestResponseDto;
import com.example.chatverse.domain.entity.Contact;
import com.example.chatverse.domain.entity.PlatformUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    // Маппинг для ContactResponseDto будет сложнее, т.к. он агрегирует данные
    // из PlatformUser и Contact. Его проще собирать в сервисе.
    // Здесь пример, если бы все данные были в одном объекте.

    @Mapping(source = "contactUser.id", target = "userId")
    @Mapping(source = "contactUser.username", target = "username")
    @Mapping(source = "contactUser.name", target = "name")
    // @Mapping(source = "contactUser.avatar", target = "avatarUrl") // если есть поле
    @Mapping(source = "online", target = "online")
    @Mapping(source = "lastSeen", target = "lastSeen")
    @Mapping(source = "contactEntity.status", target = "friendshipStatus")
    @Mapping(source = "contactEntity.updatedAt", target = "becameContactsAt") // или createdAt, в зависимости от логики
    ContactResponseDto toContactResponseDto(PlatformUser contactUser, Contact contactEntity, boolean online, LocalDateTime lastSeen);


    @Mapping(source = "contact.id", target = "contactEntityId")
    @Mapping(source = "otherUser.id", target = "otherUserId")
    @Mapping(source = "otherUser.username", target = "otherUserUsername")
    @Mapping(source = "otherUser.name", target = "otherUserName")
    // @Mapping(source = "otherUser.avatar", target = "otherUserAvatarUrl")
    @Mapping(source = "contact.status", target = "requestStatus")
    @Mapping(source = "contact.createdAt", target = "requestTimestamp")
    @Mapping(target = "direction", ignore = true) // Направление будет установлено в сервисе
    PendingRequestResponseDto toPendingRequestResponseDto(Contact contact, PlatformUser otherUser);

}