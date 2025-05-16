package com.example.chatverse.domain.service;

import com.example.chatverse.application.dto.contact.ContactResponseDto;
import com.example.chatverse.application.dto.contact.PendingRequestResponseDto;
import com.example.chatverse.application.mapper.ContactMapper;
import com.example.chatverse.domain.entity.Contact;
import com.example.chatverse.domain.entity.ContactStatus;
import com.example.chatverse.domain.entity.PlatformUser;
import com.example.chatverse.domain.repository.ContactRepository;
import com.example.chatverse.domain.repository.UserRepository;
import com.example.chatverse.infrastructure.exception.ContactLogicException;
import com.example.chatverse.infrastructure.exception.ContactNotFoundException;
import com.example.chatverse.infrastructure.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;
    private final UserService userService; // Для получения статуса онлайн
    private final SimpMessagingTemplate messagingTemplate; // Для уведомлений

    private PlatformUser getUserFromAuth(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with ID: " + userId));
    }

    private PlatformUser findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public void sendContactRequest(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new ContactLogicException("Cannot send a contact request to yourself.");
        }

        PlatformUser requester = findUserById(requesterId);
        PlatformUser targetUser = findUserById(targetUserId);

        PlatformUser userOne = requester.getId() < targetUser.getId() ? requester : targetUser;
        PlatformUser userTwo = requester.getId() < targetUser.getId() ? targetUser : requester;

        Optional<Contact> existingContactOpt = contactRepository.findContactBetweenUsers(userOne, userTwo);

        if (existingContactOpt.isPresent()) {
            Contact existingContact = existingContactOpt.get();
            if (existingContact.getStatus() == ContactStatus.ACCEPTED) {
                throw new ContactLogicException("You are already contacts.");
            }
            if (existingContact.getStatus() == ContactStatus.PENDING && existingContact.getActionUser().equals(requester)) {
                throw new ContactLogicException("Contact request already sent.");
            }
            if (existingContact.getStatus() == ContactStatus.PENDING && existingContact.getActionUser().equals(targetUser)) {
                throw new ContactLogicException("This user has already sent you a contact request. Please accept or decline it.");
            }
            if (existingContact.getStatus() == ContactStatus.BLOCKED) {
                if (existingContact.getActionUser().equals(requester)) {
                    throw new AccessDeniedException("You have blocked this user. Unblock to send a request.");
                } else {
                    throw new AccessDeniedException("This user has blocked you.");
                }
            }
            // Если был DECLINED, можно позволить отправить новый запрос, обновив существующую запись
            if (existingContact.getStatus() == ContactStatus.DECLINED) {
                existingContact.setStatus(ContactStatus.PENDING);
                existingContact.setActionUser(requester);
                existingContact.setUserOne(userOne); // Убедимся, что userOne/userTwo правильные
                existingContact.setUserTwo(userTwo);
                contactRepository.save(existingContact);
                log.info("Re-sent contact request from user {} to user {}", requesterId, targetUserId);
                // TODO: Send WebSocket notification to targetUser
                return;
            }
        }

        Contact newContactRequest = Contact.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .actionUser(requester)
                .status(ContactStatus.PENDING)
                .build();
        contactRepository.save(newContactRequest);
        log.info("Contact request sent from user {} to user {}", requesterId, targetUserId);

        // TODO: Отправить WebSocket уведомление targetUser о новом запросе
        // messagingTemplate.convertAndSendToUser(targetUserId.toString(), "/queue/contact-requests", newContactRequest);
    }

    @Transactional
    public void updateContactRequestStatus(Long currentUserId, Long otherUserId, ContactStatus newStatus) {
        if (newStatus != ContactStatus.ACCEPTED && newStatus != ContactStatus.DECLINED) {
            // Это можно оставить как IllegalArgumentException, если newStatus приходит извне и невалиден
            // или использовать ContactLogicException для консистентности
            throw new ContactLogicException("Invalid status update. Only ACCEPTED or DECLINED are allowed here.");
        }

        PlatformUser currentUser = findUserById(currentUserId);
        PlatformUser otherUser = findUserById(otherUserId);

        PlatformUser userOne = currentUser.getId() < otherUser.getId() ? currentUser : otherUser;
        PlatformUser userTwo = currentUser.getId() < otherUser.getId() ? otherUser : currentUser;

        Contact contactRequest = contactRepository.findContactBetweenUsers(userOne, userTwo)
                .orElseThrow(() -> new ContactNotFoundException("Contact request not found between users " + currentUserId + " and " + otherUserId + "."));

        if (contactRequest.getStatus() != ContactStatus.PENDING) {
            throw new ContactLogicException("This request is not pending.");
        }
        if (contactRequest.getActionUser().equals(currentUser)) {
            throw new AccessDeniedException("You cannot accept/decline your own outgoing request.");
        }

        contactRequest.setStatus(newStatus);
        contactRequest.setActionUser(currentUser);
        contactRepository.save(contactRequest);
        log.info("User {} {} contact request from user {}. New status: {}", currentUserId, newStatus.name().toLowerCase(), otherUserId, newStatus);

        // TODO: Отправить WebSocket уведомление otherUser (инициатору запроса) об изменении статуса
    }


    @Transactional
    public void removeContact(Long currentUserId, Long contactToRemoveId) {
        PlatformUser currentUser = findUserById(currentUserId);
        PlatformUser contactToRemove = findUserById(contactToRemoveId);

        PlatformUser userOne = currentUser.getId() < contactToRemove.getId() ? currentUser : contactToRemove;
        PlatformUser userTwo = currentUser.getId() < contactToRemove.getId() ? contactToRemove : currentUser;

        Contact contact = contactRepository.findContactBetweenUsers(userOne, userTwo)
                .orElseThrow(() -> new ContactNotFoundException("Contact not found between users " + currentUserId + " and " + contactToRemoveId + " to remove."));

        if (contact.getStatus() != ContactStatus.ACCEPTED) {
            throw new ContactLogicException("These users are not contacts.");
        }

        contactRepository.delete(contact);
        log.info("User {} removed user {} from contacts.", currentUserId, contactToRemoveId);
        // TODO: Отправить WebSocket уведомление contactToRemoveId
    }

    @Transactional(readOnly = true)
    public List<ContactResponseDto> getContacts(Long userId) {
        PlatformUser currentUser = findUserById(userId);
        List<Contact> acceptedContacts = contactRepository.findAllByUserAndStatus(currentUser, ContactStatus.ACCEPTED);

        if (acceptedContacts.isEmpty()) {
            return Collections.emptyList();
        }

        return acceptedContacts.stream().map(contact -> {
            PlatformUser otherUser = contact.getUserOne().equals(currentUser) ? contact.getUserTwo() : contact.getUserOne();
            boolean isOnline = userService.isUserOnline(otherUser.getId());
            LocalDateTime lastSeen = otherUser.isOnline() ? null : otherUser.getLastLogin();

            return contactMapper.toContactResponseDto(otherUser, contact, isOnline, lastSeen);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PendingRequestResponseDto> getPendingRequests(Long userId, boolean incoming) {
        PlatformUser currentUser = findUserById(userId);
        List<Contact> pendingContacts;

        if (incoming) {
            pendingContacts = contactRepository.findPendingIncomingRequestsForUser(currentUser, ContactStatus.PENDING);
        } else {
            pendingContacts = contactRepository.findAllByActionUserAndStatus(currentUser, ContactStatus.PENDING);
        }

        if (pendingContacts.isEmpty()) {
            return Collections.emptyList();
        }

        return pendingContacts.stream().map(contact -> {
            PlatformUser otherUser;
            String direction;
            if (incoming) {
                otherUser = contact.getActionUser();
                direction = "INCOMING";
            } else {
                otherUser = contact.getUserOne().equals(currentUser) ? contact.getUserTwo() : contact.getUserOne();
                direction = "OUTGOING";
            }
            PendingRequestResponseDto dto = contactMapper.toPendingRequestResponseDto(contact, otherUser);
            dto.setDirection(direction);
            return dto;
        }).collect(Collectors.toList());
    }

    // TODO: Методы для блокировки/разблокировки
    // public void blockUser(Long currentUserId, Long userToBlockId) { ... }
    // public void unblockUser(Long currentUserId, Long userToUnblockId) { ... }
    // public List<ContactResponseDto> getBlockedUsers(Long currentUserId) { ... }

    // TODO: Метод для поиска пользователей (searchUsers)
}