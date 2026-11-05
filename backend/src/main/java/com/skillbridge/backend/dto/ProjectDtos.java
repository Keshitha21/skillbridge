package com.skillbridge.backend.dto;

import com.skillbridge.backend.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

/** Project endpoints F3–F6, F9. */
public final class ProjectDtos {

    public record UpsertProjectRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 20000) String description,
            @Size(max = 20000) String goals,
            @Size(max = 60) String category,
            Project.Status status,
            @Min(1) @Max(20) Integer teamSizeMax,
            LocalDate startDate,
            LocalDate targetDeadline,
            List<SkillRef> skills,
            List<String> roles) {}

    /** helper record so owners can send skill ids with required levels */
    public record SkillRef(@NotNull Long skillId, UserSkill.Level requiredLevel) {}

    public record MemberResponse(Long userId, String fullName, String email,
                                 ProjectMember.Role role, String projectRoleTitle) {
        public static MemberResponse from(ProjectMember pm) {
            return new MemberResponse(pm.getUser().getId(), pm.getUser().getFullName(), pm.getUser().getEmail(),
                    pm.getRole(),
                    pm.getProjectRole() == null ? null : pm.getProjectRole().getTitle());
        }
    }

    public record SkillLine(Long skillId, String name, UserSkill.Level requiredLevel) {}

    public record OpenRole(Long roleId, String title, int slots, int filled) {}

    public record MilestoneDto(Long id, String title, String description, LocalDate targetDate, Milestone.Status status) {
        public static MilestoneDto from(Milestone m) {
            return new MilestoneDto(m.getId(), m.getTitle(), m.getDescription(), m.getTargetDate(), m.getStatus());
        }
    }

    /** Minimal response for create/update/status changes — avoids serializing the lazy owner proxy. */
    public record ProjectRef(Long id, String title, Project.Status status) {
        public static ProjectRef from(Project p) {
            return new ProjectRef(p.getId(), p.getTitle(), p.getStatus());
        }
    }

    public record ProjectSummary(Long id, String title, String description, String category,
                                 Project.Status status, String ownerName, Long ownerId,
                                 long memberCount, int teamSizeMax, LocalDate targetDeadline,
                                 Instant createdAt, List<String> skillNames,
                                 String myRole) {}

    public record ProjectDetail(ProjectSummary summary, String goals,
                                List<MemberResponse> members, List<SkillLine> skills,
                                List<OpenRole> openRoles, List<MilestoneDto> milestones,
                                ProgressReport progress, List<RequestLine> pendingRequests) {}

    public record RequestLine(Long requestId, Long requesterId, String requesterName, String type,
                              JoinRequest.Status status, String message, Instant createdAt) {
        public static RequestLine from(JoinRequest jr) {
            return new RequestLine(jr.getId(), jr.getRequester().getId(), jr.getRequester().getFullName(),
                    jr.getType().name(), jr.getStatus(), jr.getMessage(), jr.getCreatedAt());
        }
    }

    public record ProgressReport(
            long totalTasks, long todo, long inProgress, long blocked,
            long readyForReview, long approved, long reopened, long cancelled,
            long overdue, long pendingReviews,
            int percentComplete, List<UpcomingDeadline> upcomingDeadlines,
            List<MilestoneProgress> milestones) {}

    public record UpcomingDeadline(Long taskId, String title, String dueState) {}

    public record MilestoneProgress(Long milestoneId, String title, Milestone.Status status,
                                    long totalTasks, long completedTasks) {}
}
