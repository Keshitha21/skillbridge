package com.skillbridge.backend.dto;

import com.skillbridge.backend.entity.JoinRequest;
import com.skillbridge.backend.entity.MentorshipOffer;
import com.skillbridge.backend.entity.MentorshipRequest;
import com.skillbridge.backend.entity.MentorshipTask;
import com.skillbridge.backend.entity.Milestone;
import com.skillbridge.backend.entity.PortfolioItem;
import com.skillbridge.backend.entity.Report;
import com.skillbridge.backend.entity.Skill;
import com.skillbridge.backend.entity.Task;
import com.skillbridge.backend.entity.TaskComment;
import com.skillbridge.backend.entity.TaskReview;
import com.skillbridge.backend.entity.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests (F6), Tasks (F7–F9), collaboration (F10–F11), portfolio/mentorship (F13–F14), admin (F15) payloads. */
public final class CommonDtos {

    // ---------- requests ----------
    public record JoinRequestCreate(@Size(max = 500) String message) {}
    public record InviteCreate(@NotNull Long userId, @Size(max = 500) String message) {}
    public record RequestDecision(String note) {}
    public record JoinRequestResponse(Long id, Long projectId, String projectTitle,
                                      Long requesterId, String requesterName, String type,
                                      JoinRequest.Status status, String message, Instant createdAt,
                                      Long audienceId, String audienceName) {
        public static JoinRequestResponse from(JoinRequest jr, String projectTitle) {
            return new JoinRequestResponse(jr.getId(), jr.getProject().getId(), projectTitle,
                    jr.getRequester().getId(), jr.getRequester().getFullName(), jr.getType().name(),
                    jr.getStatus(), jr.getMessage(), jr.getCreatedAt(), null, null);
        }
    }
    public record SimpleMessage(String message) {}
    public record OkResponse(boolean ok, String message) {}

    // ---------- tasks ----------
    public record CreateTaskRequest(@NotNull Long projectId,
                                    @NotBlank @Size(max = 150) String title,
                                    @Size(max = 20000) String description,
                                    @Size(max = 600) String acceptanceCriteria,
                                    Task.Priority priority,
                                    Task.Status status,
                                    Long milestoneId,
                                    Long assigneeId,
                                    Long dependsOnId,
                                    LocalDate dueDate,
                                    @Size(max = 300) String blockerNote) {}

    public record UpdateTaskRequest(@NotBlank @Size(max = 150) String title,
                                    String description,
                                    String acceptanceCriteria,
                                    Task.Priority priority,
                                    Long milestoneId,
                                    Long assigneeId,
                                    Long dependsOnId,
                                    LocalDate dueDate,
                                    String blockerNote) {}

    public record TaskStatusRequest(@NotNull Task.Status status, @Size(max = 300) String blockerNote) {}
    public record AssigneeSuggestion(Long userId, String fullName, String matchScore, String reason) {}
    public record ReviewRequest(@NotNull TaskReview.Decision decision, @Size(max = 600) String comment) {}
    public record CommentRequest(@NotBlank @Size(max = 1000) String body) {}

    public record CommentResponse(Long id, Long taskId, Long authorId, String authorName, String body, Instant createdAt) {
        public static CommentResponse from(TaskComment c) {
            return new CommentResponse(c.getId(), c.getTask().getId(), c.getAuthor().getId(),
                    c.getAuthor().getFullName(), c.getBody(), c.getCreatedAt());
        }
    }

    public record ReviewResponse(Long id, Long reviewerId, String reviewerName, TaskReview.Decision decision, String comment, Instant createdAt) {
        public static ReviewResponse from(TaskReview r) {
            return new ReviewResponse(r.getId(), r.getReviewer().getId(), r.getReviewer().getFullName(),
                    r.getDecision(), r.getComment(), r.getCreatedAt());
        }
    }

    // ---------- milestones ----------
    public record MilestoneUpsert(@NotBlank @Size(max = 120) String title,
                                  @Size(max = 500) String description,
                                  java.time.LocalDate targetDate,
                                  Milestone.Status status) {}

    // ---------- chat & messages ----------
    public record MessageResponse(Long id, Long senderId, String senderName, String body, Instant createdAt) {}
    public record ThreadInfo(Long partnerId, String partnerName, String lastMessage,
                             Instant lastAt, long unread) {}

    // ---------- files ----------
    public record FileResponse(Long id, Long projectId, Long taskId, String originalName,
                               String mimeType, Long sizeBytes, Long uploaderId,
                               String uploaderName, Instant createdAt) {}

    // ---------- reports (U9) ----------
    public record ReportRequest(@NotNull Report.ContentType contentType, Long contentId,
                                Long reportedUserId,
                                @NotBlank @Size(max = 300) String reason,
                                @Size(max = 1000) String description) {}

    // ---------- mentorship & feedback ----------
    public record CreateOffer(@NotNull MentorshipOffer.Type type,
                              @NotBlank @Size(max = 120) String title,
                              @Size(max = 20000) String description,
                              Long skillId,
                              @Min(1) @Max(20) Integer maxParticipants) {}

    public record OfferResponse(Long id, Long ownerId, String ownerName, Long skillId, String skillName,
                                MentorshipOffer.Type type, String title, String description,
                                int maxParticipants, int activeParticipants, MentorshipOffer.Status status,
                                Instant createdAt) {}

    public record CreateMentorshipRequest(@Size(max = 500) String message) {}
    public record MentorshipRequestResponse(Long id, Long offerId, String offerTitle, String offerOwnerName,
                                            Long offerOwnerId, MentorshipOffer.Type offerType,
                                            Long requesterId, String requesterName,
                                            String message, MentorshipRequest.Status status, Instant createdAt) {}

    public record CreateFeedback(@NotNull @Min(1) @Max(5) int rating, @Size(max = 600) String comment) {}
    public record FeedbackResponse(Long id, Long offerId, Long fromUserId, String fromName,
                                   Long toUserId, String toName, int rating, String comment, Instant createdAt) {}

    // ---------- mentorship workspace: teaching files & learning-plan tasks (U8/F13) ----------

    public record CreateMentorshipTask(@NotBlank @Size(max = 150) String title, Long assigneeId) {}
    public record MentorshipTaskStatus(@NotNull MentorshipTask.Status status) {}

    public record MentorshipTaskView(Long id, Long offerId, String title, MentorshipTask.Status status,
                                     Long assigneeId, String assigneeName,
                                     Long createdById, String createdByName,
                                     Instant createdAt, Instant updatedAt) {}

    public record MentorshipFileView(Long id, Long offerId, Long uploaderId, String uploaderName,
                                     String fileName, String mimeType, Long sizeBytes, Instant createdAt) {}

    // ---------- portfolio ----------
    public record CreatePortfolioItem(@NotBlank @Size(max = 150) String title,
                                      @Size(max = 20000) String description,
                                      PortfolioItem.Tag tag,
                                      Long projectId,
                                      @Size(max = 300) String link,
                                      PortfolioItem.Visibility visibility) {}

    public record PortfolioItemResponse(Long id, Long ownerId, String ownerName, Long projectId,
                                        String projectTitle, String title, String description,
                                        PortfolioItem.Tag tag, String link,
                                        PortfolioItem.Visibility visibility, Instant createdAt) {}

    // ---------- notifications ----------
    public record NotificationList(List<NotificationResponse> items, long unreadCount) {}
    public record NotificationResponse(Long id, String type, String title, String body, String link,
                                       boolean read, Instant createdAt) {}

    // ---------- skills catalogue ----------
    public record SkillDto(Long id, String name, String description, Long categoryId, String categoryName) {
        public static SkillDto from(com.skillbridge.backend.entity.Skill s) {
            return new SkillDto(s.getId(), s.getName(), s.getDescription(),
                    s.getCategory() == null ? null : s.getCategory().getId(),
                    s.getCategory() == null ? null : s.getCategory().getName());
        }
    }

    // ---------- admin moderation views (mapped inside the transactional session) ----------
    public record ReportView(Long id, Long reporterId, String reporterName, Long reportedUserId,
                             String reportedUserName, String contentType, Long contentId, String reason,
                             String description, com.skillbridge.backend.entity.Report.Status status,
                             String adminNote, Instant reviewedAt, Instant createdAt) {
        public static ReportView from(com.skillbridge.backend.entity.Report r) {
            return new ReportView(r.getId(),
                    r.getReporter() == null ? null : r.getReporter().getId(),
                    r.getReporter() == null ? null : r.getReporter().getFullName(),
                    r.getReportedUser() == null ? null : r.getReportedUser().getId(),
                    r.getReportedUser() == null ? null : r.getReportedUser().getFullName(),
                    r.getContentType().name(), r.getContentId(), r.getReason(), r.getDescription(),
                    r.getStatus(), r.getAdminNote(), r.getReviewedAt(), r.getCreatedAt());
        }
    }

    public record AuditView(Long id, Long adminId, String adminName, String action, String entityType,
                            Long entityId, String reason, Instant createdAt) {
        public static AuditView from(com.skillbridge.backend.entity.AdminAuditLog a) {
            return new AuditView(a.getId(), a.getAdmin().getId(), a.getAdmin().getFullName(),
                    a.getAction(), a.getEntityType(), a.getEntityId(), a.getReason(), a.getCreatedAt());
        }
    }

    public record SkillCategoryDto(Long id, String name, String description) {}
    public record SkillUpsert(@NotBlank @Size(max = 80) String name,
                              @Size(max = 300) String description,
                              @NotNull Long categoryId) {}
    public record SkillCategoryUpsert(@NotBlank @Size(max = 80) String name, @Size(max = 300) String description) {}

    // ---------- admin ----------
    public record UserStatusUpdate(@NotNull com.skillbridge.backend.entity.User.Status status,
                                   @NotBlank @Size(max = 500) String reason) {}
    public record ReportReview(@NotNull Report.Status status, @Size(max = 500) String adminNote) {}
    public record StatsSummary(long users, long activeUsers, long projects, long publishedProjects,
                               long tasks, long openReports, long mentorshipOffers) {}
}
