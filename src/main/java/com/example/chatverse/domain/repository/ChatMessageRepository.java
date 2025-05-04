package com.example.chatverse.domain.repository;

import com.example.chatverse.domain.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * Находит все сообщения для указанной комнаты чата, отсортированные по времени.
     * @param roomId Идентификатор комнаты чата.
     * @return Список сообщений.
     */
    List<ChatMessageEntity> findByRoomIdOrderByTimestampAsc(String roomId);

    // Можно добавить другие методы поиска, например, по отправителю, получателю, дате и т.д.
}