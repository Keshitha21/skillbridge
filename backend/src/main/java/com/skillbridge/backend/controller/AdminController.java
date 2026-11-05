package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.dto.ProjectDtos;
import com.skillbridge.backend.entity.AdminAuditLog;
import com.skillbridge.backend.entity.Report;
import com.skillbridge.backend.entity.Skill;
import com.skillbridge.backend.entity.SkillCategory;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.AdminService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** U9 / F15 — every endpoint here is already restricted to ROLE_ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    public record UserView(Long id, String fullName, String email, User.Role role, User.Status status) {
        public static UserView from(User u) {
            return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus());
        }
    }

    public record StatusPayload(@NotNull User.Status status, @NotBlank String reason) {}

    // ---------- accounts ----------
    @GetMapping("/users")
    public ResponseEntity<List<UserView>> users(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(admin.users(q).stream().map(UserView::from).toList());
    }

    @PostMapping("/users/{id}/status")
    public ResponseEntity<CommonDtos.OkResponse> setStatus(@AuthenticationPrincipal User adminUser,
                                                           @PathVariable Long id,
                                                           @jakarta.validation.Valid @RequestBody StatusPayload payload) {
        admin.setAccountStatus(adminUser, id, payload.status(), payload.reason());
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Account status updated."));
    }

    // ---------- skill categories ----------
    @GetMapping("/skill-categories")
    public ResponseEntity<List<SkillCategory>> categories() {
        return ResponseEntity.ok(admin.categories());
    }

    @PostMapping("/skill-categories")
    public ResponseEntity<SkillCategory> addCategory(@AuthenticationPrincipal User adminUser,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(admin.addCategory(adminUser,
                new CommonDtos.SkillCategoryUpsert(body.getOrDefault("name", ""), body.get("description"))));
    }

    @DeleteMapping("/skill-categories/{id}")
    public ResponseEntity<CommonDtos.OkResponse> deleteCategory(@AuthenticationPrincipal User adminUser,
                                                                @PathVariable Long id) {
        admin.deleteCategory(adminUser, id);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Category deleted."));
    }

    // ---------- skills ----------
    @GetMapping("/skills")
    public ResponseEntity<List<CommonDtos.SkillDto>> skills(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(admin.skillsIn(categoryId));
    }

    @PostMapping("/skills")
    public ResponseEntity<CommonDtos.SkillDto> addSkill(@AuthenticationPrincipal User adminUser,
                                                        @jakarta.validation.Valid @RequestBody CommonDtos.SkillUpsert request) {
        return ResponseEntity.ok(admin.addSkill(adminUser, request));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<CommonDtos.SkillDto> renameSkill(@AuthenticationPrincipal User adminUser, @PathVariable Long id,
                                                           @jakarta.validation.Valid @RequestBody CommonDtos.SkillUpsert request) {
        return ResponseEntity.ok(admin.renameSkill(adminUser, id, request));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<CommonDtos.OkResponse> deleteSkill(@AuthenticationPrincipal User adminUser,
                                                             @PathVariable Long id) {
        admin.deleteSkill(adminUser, id);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Skill removed from the catalogue."));
    }

    public record ReportView(Long id, Long reporterId, String reporterName, Long reportedUserId,
                             String reportedUserName, String contentType, Long contentId, String reason,
                             String description, Report.Status status, String adminNote,
                             Instant reviewedAt, Instant createdAt) {
        public static ReportView from(Report r) {
            return new ReportView(r.getId(), r.getReporter() == null ? null : r.getReporter().getId(),
                    r.getReporter() == null ? null : r.getReporter().getFullName(),
                    r.getReportedUser() == null ? null : r.getReportedUser().getId(),
                    r.getReportedUser() == null ? null : r.getReportedUser().getFullName(),
                    r.getContentType().name(), r.getContentId(), r.getReason(), r.getDescription(),
                    r.getStatus(), r.getAdminNote(), r.getReviewedAt(), r.getCreatedAt());
        }
    }

    public record AuditView(Long id, Long adminId, String adminName, String action, String entityType,
                            Long entityId, String reason, Instant createdAt) {
        public static AuditView from(AdminAuditLog a) {
            return new AuditView(a.getId(), a.getAdmin().getId(), a.getAdmin().getFullName(),
                    a.getAction(), a.getEntityType(), a.getEntityId(), a.getReason(), a.getCreatedAt());
        }
    }

    // ---------- reports review ----------
    @GetMapping("/reports")
    public ResponseEntity<List<CommonDtos.ReportView>> reports(@RequestParam(required = false) Report.Status status) {
        return ResponseEntity.ok(admin.reports(status));
    }

    @PostMapping("/reports/{id}/review")
    public ResponseEntity<CommonDtos.OkResponse> reviewReport(@AuthenticationPrincipal User adminUser,
                                                              @PathVariable Long id,
                                                              @jakarta.validation.Valid @RequestBody CommonDtos.ReportReview decision) {
        admin.reviewReport(adminUser, id, decision);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Report reviewed."));
    }

    // ---------- audit log & stats ----------
    @GetMapping("/audit-log")
    public ResponseEntity<List<CommonDtos.AuditView>> audit() {
        return ResponseEntity.ok(admin.auditLog());
    }

    @GetMapping("/stats")
    public ResponseEntity<CommonDtos.StatsSummary> stats() {
        return ResponseEntity.ok(admin.stats());
    }
}
