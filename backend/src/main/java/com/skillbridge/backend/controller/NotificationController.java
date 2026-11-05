package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.Notification;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** U7 / F12: notifications list, unread counter, mark as read. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    public static CommonDtos.NotificationResponse toDto(Notification n) {
        return new CommonDtos.NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getLink(), n.isReadFlag(), n.getCreatedAt());
    }

    @GetMapping
    public ResponseEntity<CommonDtos.NotificationList> list(@AuthenticationPrincipal User me) {
        List<CommonDtos.NotificationResponse> items = notifications.listMine(me.getId()).stream()
                .map(NotificationController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(new CommonDtos.NotificationList(items, notifications.unreadCount(me.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<CommonDtos.OkResponse> read(@AuthenticationPrincipal User me, @PathVariable Long id) {
        notifications.markRead(me.getId(), id);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Notification marked as read."));
    }

    @PostMapping("/read-all")
    public ResponseEntity<CommonDtos.OkResponse> readAll(@AuthenticationPrincipal User me) {
        notifications.markAllRead(me.getId());
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "All notifications marked as read."));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unread(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(notifications.unreadCount(me.getId()));
    }
}
