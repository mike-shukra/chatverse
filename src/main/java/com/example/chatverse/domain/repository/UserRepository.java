package com.example.chatverse.domain.repository;

import com.example.chatverse.domain.entity.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<PlatformUser, Long> {
    Optional<PlatformUser> findByPhone(String phone);
    Optional<PlatformUser> findByUsername(String username);

    // Проверка существования пользователя по телефону
    boolean existsByPhone(String phone);

    // Проверка существования пользователя по имени пользователя
    boolean existsByUsername(String username);

    // Удаление пользователя по номеру телефона
    void deleteByPhone(String phone);

    // Удаление пользователя по имени пользователя
    void deleteByUsername(String username);

    // Получение списка пользователей по роли
    List<PlatformUser> findAllByRole(String role);

    // Поиск пользователей, зарегистрированных после определённой даты
    List<PlatformUser> findByCreatedDateAfter(LocalDateTime date);

    // Поиск пользователей, у которых имя пользователя содержит определённую строку
    List<PlatformUser> findByUsernameContainingIgnoreCase(String usernameFragment);

    // Поиск всех активных пользователей
    List<PlatformUser> findByIsActiveTrue();

    // Поиск всех неактивных пользователей
    List<PlatformUser> findByIsActiveFalse();
}

