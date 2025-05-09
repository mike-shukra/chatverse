package com.example.chatverse.domain.entity;

public enum ContactStatus {
    /**
     * Запрос на добавление в контакты отправлен и ожидает ответа.
     * userOne отправил запрос userTwo.
     */
    PENDING,

    /**
     * Запрос на добавление в контакты принят.
     * Пользователи являются контактами друг друга.
     */
    ACCEPTED,

    /**
     * Запрос на добавление в контакты отклонен.
     */
    DECLINED,

    /**
     * Один пользователь заблокировал другого.
     * Если userOne заблокировал userTwo, то actionUserId будет userOne.
     */
    BLOCKED
}