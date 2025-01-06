package com.example.chatverse.application.mapper;

import com.example.chatverse.application.dto.request.RegisterIn;
import com.example.chatverse.application.dto.request.UserUpdateRequest;
import com.example.chatverse.domain.entity.PlatformUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserMapper {
    public static PlatformUser registerInToEntity(RegisterIn request) {
        return PlatformUser.builder()
                .phone(request.getPhone())
                .name(request.getName())
                .username(request.getUsername())
                .created(LocalDateTime.now())
                .build();
    }
    public static PlatformUser phoneToEntity(String phone) {
        return PlatformUser.builder()
                .phone(phone)
                .username(phone)
                .created(LocalDateTime.now())
                .online(false)
                .completedTask(0)
                .build();
    }
    public static PlatformUser updateEntityFromRequest(UserUpdateRequest request, PlatformUser user) {
        return PlatformUser.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .username(request.getUsername() != null ? request.getUsername() : user.getUsername())
                .name(request.getName() != null ? request.getName() : user.getName())
                .birthday(request.getBirthday() != null ? LocalDate.parse(request.getBirthday(), DateTimeFormatter.ISO_DATE) : user.getBirthday())
                .city(request.getCity() != null ? request.getCity() : user.getCity())
                .vk(request.getVk() != null ? request.getVk() : user.getVk())
                .instagram(request.getInstagram() != null ? request.getInstagram() : user.getInstagram())
                .status(request.getStatus() != null ? request.getStatus() : user.getStatus())
                .lastLogin(user.getLastLogin())
                .online(user.isOnline())
                .created(user.getCreated())
                .completedTask(user.getCompletedTask())
                .build();
    }
}
