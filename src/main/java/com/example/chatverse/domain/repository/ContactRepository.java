package com.example.chatverse.domain.repository;

import com.example.chatverse.domain.entity.Contact;
import com.example.chatverse.domain.entity.ContactStatus;
import com.example.chatverse.domain.entity.PlatformUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Находит существующую связь между двумя пользователями, независимо от того, кто userOne, а кто userTwo.
     */
    @Query("SELECT c FROM Contact c WHERE (c.userOne = :userA AND c.userTwo = :userB) OR (c.userOne = :userB AND c.userTwo = :userA)")
    Optional<Contact> findContactBetweenUsers(@Param("userA") PlatformUser userA, @Param("userB") PlatformUser userB);

    /**
     * Находит все контакты пользователя с определенным статусом.
     * Учитывает случаи, когда пользователь может быть как userOne, так и userTwo.
     */
    @Query("SELECT c FROM Contact c WHERE (c.userOne = :user OR c.userTwo = :user) AND c.status = :status")
    List<Contact> findAllByUserAndStatus(@Param("user") PlatformUser user, @Param("status") ContactStatus status);

    /**
     * Находит все входящие запросы для пользователя (где он userTwo, а запрос PENDING и инициирован другим).
     * Или где он userOne, а запрос PENDING и инициирован другим (если actionUser не совпадает с user).
     * Более точный запрос: Находит PENDING запросы, где actionUser не является текущим пользователем.
     */
    @Query("SELECT c FROM Contact c WHERE ((c.userOne = :user AND c.actionUser = c.userTwo) OR (c.userTwo = :user AND c.actionUser = c.userOne)) AND c.status = :status")
    List<Contact> findPendingIncomingRequestsForUser(@Param("user") PlatformUser user, @Param("status") ContactStatus status);


    /**
     * Находит все исходящие PENDING запросы от пользователя (где он является actionUser).
     */
    List<Contact> findAllByActionUserAndStatus(PlatformUser actionUser, ContactStatus status);

}