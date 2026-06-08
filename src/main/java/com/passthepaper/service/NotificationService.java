package com.passthepaper.service;

import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;

    public void send(User user, Notification.NotificationType type, String title, String message, UUID relatedId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(false);
        n.setRelatedId(relatedId);
        notifRepo.save(n);
    }

    public List<Notification> getForUser(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return notifRepo.findByUserOrderByCreatedAtDesc(user);
    }

    public long countUnread(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return notifRepo.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markRead(UUID notifId, UUID userId) {
        Notification n = notifRepo.findById(notifId).orElseThrow(() -> new AppException("Not found"));
        if (!n.getUser().getId().equals(userId)) throw new AppException("Not your notification");
        n.setIsRead(true);
        notifRepo.save(n);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        notifRepo.markAllReadByUser(user);
    }

    @Transactional
    public void delete(UUID notifId, UUID userId) {
        Notification n = notifRepo.findById(notifId).orElseThrow(() -> new AppException("Not found"));
        if (!n.getUser().getId().equals(userId)) throw new AppException("Not your notification");
        notifRepo.delete(n);
    }

    @Transactional
    public void deleteAll(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow();
        notifRepo.deleteByUser(user);
    }
}
