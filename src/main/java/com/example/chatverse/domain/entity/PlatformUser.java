package com.example.chatverse.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "platform_users")
public class PlatformUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "city")
    private String city;

    @Column(name = "vk")
    private String vk;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "status")
    private String status;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "online", nullable = false)
    private boolean online;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "completed_task", nullable = false)
    private int completedTask;

}

