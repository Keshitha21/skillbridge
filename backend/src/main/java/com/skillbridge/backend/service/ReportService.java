package com.skillbridge.backend.service;

import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.ReportRepository;
import com.skillbridge.backend.repository.TaskRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Reports about users or content flow into the admin review queue (F15, NFR security). */
@Service
public class ReportService {

    private final ReportRepository reports;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final NotificationService notifier;

    public ReportService(ReportRepository reports, UserRepository users, ProjectRepository projects,
                         TaskRepository tasks, NotificationService notifier) {
        this.reports = reports;
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.notifier = notifier;
    }

    public record ReportData(com.skillbridge.backend.entity.Report.ContentType contentType, Long contentId,
                             Long reportedUserId, String reason, String description) {}

    @Transactional
    public com.skillbridge.backend.entity.Report create(User reporter, Map<String, Object> payload) {
        String type = String.valueOf(payload.getOrDefault("contentType", "OTHER"));
        com.skillbridge.backend.entity.Report.ContentType contentType;
        try {
            contentType = com.skillbridge.backend.entity.Report.ContentType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported report type.");
        }
        String reason = String.valueOf(payload.getOrDefault("reason", "")).trim();
        if (reason.isBlank()) {
            throw new BadRequestException("Tell us briefly why you are reporting this content.");
        }
        Long contentId = toLong(payload.get("contentId"));
        Long reportedUserId = toLong(payload.get("reportedUserId"));
        if (reportedUserId == null && contentId != null && contentType == com.skillbridge.backend.entity.Report.ContentType.USER) {
            reportedUserId = contentId;
        }
        if (reportedUserId != null && reportedUserId.equals(reporter.getId())) {
            throw new BadRequestException("You cannot report your own account.");
        }

        com.skillbridge.backend.entity.Report saved = reports.save(com.skillbridge.backend.entity.Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUserId == null ? null : users.findById(reportedUserId).orElse(null))
                .contentType(contentType)
                .contentId(contentId)
                .reason(reason)
                .description(String.valueOf(payload.getOrDefault("description", "")))
                .status(com.skillbridge.backend.entity.Report.Status.PENDING)
                .build());
        // notify administrators so important reports are surfaced quickly
        users.findAll().stream().filter(u -> u.getRole() == com.skillbridge.backend.entity.User.Role.ADMIN)
                .forEach(admin -> notifier.notify(admin, NotificationService.Type.PROJECT_UPDATE,
                        "New report submitted", "A " + contentType.name() + " report is waiting for review.",
                        "/admin"));
        return saved;
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
