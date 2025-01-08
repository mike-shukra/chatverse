package com.example.chatverse.application.mapper;

import com.example.chatverse.application.dto.request.RegisterIn;
import com.example.chatverse.application.dto.request.UserUpdateRequest;
import com.example.chatverse.application.service.UsernameGenerator;
import com.example.chatverse.domain.entity.PlatformUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserMapper {

    public static PlatformUser registerInToEntity(RegisterIn request) {
        return PlatformUser.builder()
                .phone(request.getPhone())
                .username(request.getUsername())
                .name(request.getName())
                .created(LocalDateTime.now())
                .online(false)
                .completedTask(0)
                .active(true)
                .build();
    }

    public static PlatformUser phoneToEntity(String phone) {
        String username = "";
        try {
            username = UsernameGenerator.generateUsernameFromPhone(phone);
            System.out.println("username: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlatformUser platformUser = PlatformUser.builder()
                .name("name")
                .phone(phone)
                .username(username)
                .created(LocalDateTime.now())
                .online(false)
                .completedTask(0)
                .active(true)
                .build();
        System.out.println("platformUser: " + platformUser);
        return platformUser;
    }

    public static PlatformUser updateEntityFromRequest(UserUpdateRequest request, PlatformUser user) {
        PlatformUser platformUser = PlatformUser.builder()
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
                .role(user.getRole() != null ? user.getRole() : "user")
                .active(user.isActive())
                .build();
        System.out.println("platformUser: " + platformUser);
        return platformUser;
    }
}
