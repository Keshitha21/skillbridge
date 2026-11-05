package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.AdminAuditLog;
import com.skillbridge.backend.entity.Report;
import com.skillbridge.backend.entity.Skill;
import com.skillbridge.backend.entity.SkillCategory;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.AdminAuditLogRepository;
import com.skillbridge.backend.repository.FeedbackRepository;
import com.skillbridge.backend.repository.MentorshipOfferRepository;
import com.skillbridge.backend.repository.NotificationRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.ReportRepository;
import com.skillbridge.backend.repository.SkillCategoryRepository;
import com.skillbridge.backend.repository.SkillRepository;
import com.skillbridge.backend.repository.TaskRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** U9 / F15: admin account management, skill categories, report reviews and the audit log. */
@Service
public class AdminService {

    private final UserRepository users;
    private final SkillCategoryRepository categories;
    private final SkillRepository skills;
    private final ReportRepository reports;
    private final AdminAuditLogRepository audit;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final MentorshipOfferRepository offers;
    private final FeedbackRepository feedbacks;
    private final NotificationService notifier;

    public AdminService(UserRepository users, SkillCategoryRepository categories, SkillRepository skills,
                        ReportRepository reports, AdminAuditLogRepository audit, ProjectRepository projects,
                        TaskRepository tasks, MentorshipOfferRepository offers, FeedbackRepository feedbacks,
                        NotificationService notifier) {
        this.users = users;
        this.categories = categories;
        this.skills = skills;
        this.reports = reports;
        this.audit = audit;
        this.projects = projects;
        this.tasks = tasks;
        this.offers = offers;
        this.feedbacks = feedbacks;
        this.notifier = notifier;
    }

    // ---------- accounts ----------

    @Transactional
    public List<User> users(String q) {
        if (q != null && !q.isBlank()) {
            return users.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q);
        }
        return users.findAll();
    }

    @Transactional
    public void setAccountStatus(User admin, Long userId, User.Status status, String reason) {
        User target = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (target.getId().equals(admin.getId())) {
            throw new BadRequestException("Administrators cannot change their own account status.");
        }
        target.setStatus(status);
        users.save(target);
        log(admin, "SET_ACCOUNT_STATUS", "USER", target.getId(),
                reason + " (account now " + status + ")");
        if (status == User.Status.ACTIVE) {
            notifier.notify(target, NotificationService.Type.PROJECT_UPDATE, "Account reactivated",
                    "Your account was reactivated by an administrator.", "/dashboard");
        }
    }

    // ---------- skill categories & skills ----------

    @Transactional
    public List<SkillCategory> categories() {
        return categories.findAllByOrderByNameAsc();
    }

    @Transactional
    public SkillCategory addCategory(User admin, CommonDtos.SkillCategoryUpsert request) {
        if (categories.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("This skill category already exists.");
        }
        SkillCategory saved = categories.save(SkillCategory.builder()
                .name(request.name().trim())
                .description(request.description())
                .build());
        log(admin, "ADD_SKILL_CATEGORY", "SKILL_CATEGORY", saved.getId(), request.name());
        return saved;
    }

    @Transactional
    public void deleteCategory(User admin, Long categoryId) {
        SkillCategory category = categories.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill category not found."));
        boolean inUse = skills.findAllByOrderByNameAsc().stream()
                .anyMatch(s -> s.getCategory() != null && s.getCategory().getId().equals(categoryId));
        if (inUse) {
            throw new BadRequestException("Reassign the skills in this category before deleting it.");
        }
        categories.delete(category);
        log(admin, "DELETE_SKILL_CATEGORY", "SKILL_CATEGORY", categoryId, category.getName());
    }

    @Transactional
    public List<CommonDtos.SkillDto> skillsIn(Long categoryId) {
        List<Skill> list = categoryId == null
                ? skills.findAllByOrderByNameAsc()
                : skills.findByCategoryIdOrderByNameAsc(categoryId);
        return list.stream().map(CommonDtos.SkillDto::from).toList();
    }

    @Transactional
    public CommonDtos.SkillDto addSkill(User admin, CommonDtos.SkillUpsert request) {
        if (skills.findByNameIgnoreCase(request.name()).isPresent()) {
            throw new BadRequestException("This skill already exists.");
        }
        SkillCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("Pick a valid skill category."));
        Skill saved = skills.save(Skill.builder()
                .category(category)
                .name(request.name().trim())
                .description(request.description())
                .build());
        log(admin, "ADD_SKILL", "SKILL", saved.getId(), saved.getName());
        return CommonDtos.SkillDto.from(saved);
    }

    @Transactional
    public CommonDtos.SkillDto renameSkill(User admin, Long skillId, CommonDtos.SkillUpsert request) {
        Skill skill = skills.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found."));
        SkillCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("Pick a valid skill category."));
        skill.setName(request.name().trim());
        skill.setDescription(request.description());
        skill.setCategory(category);
        Skill saved = skills.save(skill);
        log(admin, "UPDATE_SKILL", "SKILL", skillId, saved.getName());
        return CommonDtos.SkillDto.from(saved);
    }

    @Transactional
    public void deleteSkill(User admin, Long skillId) {
        Skill skill = skills.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found."));
        skills.delete(skill);
        log(admin, "DELETE_SKILL", "SKILL", skillId, skill.getName());
    }

    // ---------- reports review ----------

    @Transactional
    public List<CommonDtos.ReportView> reports(Report.Status status) {
        List<Report> list = status == null
                ? reports.findAllByOrderByCreatedAtDesc()
                : reports.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(CommonDtos.ReportView::from).toList();
    }

    @Transactional
    public void reviewReport(User admin, Long reportId, CommonDtos.ReportReview decision) {
        Report report = reports.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        report.setStatus(decision.status());
        report.setAdminNote(decision.adminNote());
        report.setReviewedBy(admin);
        report.setReviewedAt(Instant.now());
        reports.save(report);
        log(admin, "REVIEW_REPORT", "REPORT", reportId,
                decision.status() + (decision.adminNote() == null ? "" : " — " + decision.adminNote()));
    }

    // ---------- audit ----------

    @Transactional(readOnly = true)
    public List<CommonDtos.AuditView> auditLog() {
        return audit.findTop100ByOrderByCreatedAtDesc().stream()
                .map(CommonDtos.AuditView::from).toList();
    }

    @Transactional
    public void log(User admin, String action, String entityType, Long entityId, String reason) {
        audit.save(AdminAuditLog.builder()
                .admin(admin)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .reason(reason)
                .build());
    }

    @Transactional(readOnly = true)
    public CommonDtos.StatsSummary stats() {
        return new CommonDtos.StatsSummary(
                users.count(),
                users.findAll().stream().filter(u -> u.getStatus() == User.Status.ACTIVE).count(),
                projects.count(),
                projects.findAll().stream().filter(p -> p.getStatus() == com.skillbridge.backend.entity.Project.Status.PUBLISHED).count(),
                tasks.count(),
                reports.findAll().stream().filter(r -> r.getStatus() == Report.Status.PENDING).count(),
                offers.count());
    }
}
