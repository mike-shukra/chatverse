package com.example.chatverse.application.mapper;

import com.example.chatverse.application.dto.message.ChatMessage;
import com.example.chatverse.domain.entity.ChatMessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring") // Используем componentModel = "spring" для интеграции со Spring
public interface ChatMessageMapper {

    ChatMessageMapper INSTANCE = Mappers.getMapper(ChatMessageMapper.class);

    @Mapping(target = "id", ignore = true) // ID генерируется базой данных
    ChatMessageEntity toEntity(ChatMessage dto);

    ChatMessage toDto(ChatMessageEntity entity);

    List<ChatMessage> toDtoList(List<ChatMessageEntity> entities);
}