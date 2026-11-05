package com.skillbridge.backend.service;

import com.skillbridge.backend.entity.Notification;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * U7 / F12: creates notifications for project events.
 * A failing notification must never undo a saved project update (SRS note).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public enum Type {
        JOIN_REQUEST, REQUEST_DECIDED, INVITE_RECEIVED, INVITE_DECIDED,
        TASK_ASSIGNED, TASK_REVIEWED, MEMBER_REMOVED, MEMBER_ADDED,
        COMMENT_ADDED, FILE_UPLOADED, NEW_MESSAGE, PROJECT_UPDATE,
        MENTORSHIP_REQUEST, MENTORSHIP_DECIDED, MENTORSHIP_COMPLETED, FEEDBACK_RECEIVED
    }

    public record NotificationSummary(Instant createdAt) {}

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(User recipient, Type type, String title, String body, String link) {
        try {
            if (recipient == null) return;
            repo.save(Notification.builder()
                    .user(recipient)
                    .type(type.name())
                    .title(title)
                    .body(body)
                    .link(link)
                    .build());
        } catch (Exception ex) {
            log.warn("Notification could not be saved: {}", ex.getMessage());
        }
    }

    /** Notify many users; failures are isolated per recipient. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAll(List<User> recipients, Type type, String title, String body, String link) {
        if (recipients == null) return;
        for (User recipient : recipients) {
            notify(recipient, type, title, body, link);
        }
    }

    @Transactional
    public List<Notification> listMine(Long userId) {
        return repo.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public long unreadCount(Long userId) {
        return repo.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new com.skillbridge.backend.exception.ResourceNotFoundException("Notification not found."));
        if (!n.getUser().getId().equals(userId)) {
            throw new com.skillbridge.backend.exception.ForbiddenException("You cannot view someone else's notification.");
        }
        n.setReadFlag(true);
        repo.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllRead(userId);
    }
}
