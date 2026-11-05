package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.*;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.ProjectSkillRepository;
import com.skillbridge.backend.repository.TaskCommentRepository;
import com.skillbridge.backend.repository.TaskRepository;
import com.skillbridge.backend.repository.TaskReviewRepository;
import com.skillbridge.backend.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** U4 / F7–F8: task planning, assignment, status updates and review decisions. */
@Service
public class TaskService {

    public record TaskView(Long id, Long projectId, String projectTitle, Long milestoneId, String milestoneTitle,
                           Long createdById, String createdByName, Long assigneeId, String assigneeName,
                           String title, String description, String acceptanceCriteria, Task.Priority priority,
                           Task.Status status, LocalDate dueDate, Long dependsOnId, String blockerNote,
                           java.time.Instant completedAt, java.time.Instant createdAt, java.time.Instant updatedAt) {
        public static TaskView from(Task t) {
            return new TaskView(t.getId(), t.getProject().getId(), t.getProject().getTitle(),
                    t.getMilestone() == null ? null : t.getMilestone().getId(),
                    t.getMilestone() == null ? null : t.getMilestone().getTitle(),
                    t.getCreatedBy().getId(), t.getCreatedBy().getFullName(),
                    t.getAssignee() == null ? null : t.getAssignee().getId(),
                    t.getAssignee() == null ? null : t.getAssignee().getFullName(),
                    t.getTitle(), t.getDescription(), t.getAcceptanceCriteria(), t.getPriority(), t.getStatus(),
                    t.getDueDate(), t.getDependsOn() == null ? null : t.getDependsOn().getId(),
                    t.getBlockerNote(), t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final ProjectMemberRepository memberRepo;
    private final ProjectSkillRepository projectSkills;
    private final UserSkillRepository userSkills;
    private final TaskCommentRepository comments;
    private final TaskReviewRepository reviews;
    private final com.skillbridge.backend.repository.MilestoneRepository milestoneRepo;
    private final ProjectAccess access;
    private final NotificationService notifier;

    public TaskService(TaskRepository tasks, ProjectRepository projects, ProjectMemberRepository memberRepo,
                       ProjectSkillRepository projectSkills, UserSkillRepository userSkills,
                       TaskCommentRepository comments, TaskReviewRepository reviews,
                       com.skillbridge.backend.repository.MilestoneRepository milestoneRepo,
                       ProjectAccess access, NotificationService notifier) {
        this.tasks = tasks;
        this.projects = projects;
        this.memberRepo = memberRepo;
        this.projectSkills = projectSkills;
        this.userSkills = userSkills;
        this.comments = comments;
        this.reviews = reviews;
        this.milestoneRepo = milestoneRepo;
        this.access = access;
        this.notifier = notifier;
    }

    // ---------- listing ----------

    @Transactional(readOnly = true)
    public List<TaskView> projectTasks(User requester, Long projectId) {
        access.requireMember(requester, projectId);
        return tasks.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(TaskView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskView> myTasks(User me) {
        return tasks.findByAssigneeIdOrderByUpdatedAtDesc(me.getId()).stream().map(TaskView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskView> managedTasks(User manager) {
        var map = new LinkedHashMap<Long, Task>();
        for (ProjectMember pm : memberRepo.findByUserId(manager.getId())) {
            if (pm.getRole() != ProjectMember.Role.OWNER && pm.getRole() != ProjectMember.Role.MANAGER) continue;
            for (Task t : tasks.findByProjectId(pm.getProject().getId())) {
                map.put(t.getId(), t);
            }
        }
        return map.values().stream().map(TaskView::from).toList();
    }

    // ---------- create / update (managers) ----------

    @Transactional
    public TaskView create(User manager, CommonDtos.CreateTaskRequest request) {
        access.requireManager(manager, request.projectId());
        Project project = projects.findById(request.projectId()).orElseThrow();
        User assignee = resolveAssignee(project, request.assigneeId());
        Milestone milestone = resolveMilestone(project, request.milestoneId());
        Task dependsOn = resolveDependency(null, request.dependsOnId(), project.getId());

        Task task = tasks.save(Task.builder()
                .project(project)
                .milestone(milestone)
                .createdBy(manager)
                .assignee(assignee)
                .title(request.title().trim())
                .description(request.description())
                .acceptanceCriteria(request.acceptanceCriteria())
                .priority(request.priority() == null ? Task.Priority.MEDIUM : request.priority())
                .status(Task.Status.TODO)
                .dependsOn(dependsOn)
                .dueDate(request.dueDate())
                .blockerNote(request.blockerNote())
                .build());

        if (assignee != null) {
            notifier.notify(assignee, NotificationService.Type.TASK_ASSIGNED, "New task assigned to you",
                    manager.getFullName() + " assigned you \"" + task.getTitle() + "\" in \"" + project.getTitle() + "\".",
                    "/tasks?project=" + project.getId());
        }
        return TaskView.from(task);
    }

    @Transactional
    public TaskView update(User manager, Long taskId, CommonDtos.UpdateTaskRequest request) {
        Task task = load(taskId);
        access.requireManager(manager, task.getProject().getId());
        User assignee = resolveAssignee(task.getProject(), request.assigneeId());
        Milestone milestone = resolveMilestone(task.getProject(), request.milestoneId());
        Task dependsOn = resolveDependency(task, request.dependsOnId(), task.getProject().getId());

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setAcceptanceCriteria(request.acceptanceCriteria());
        task.setPriority(request.priority() == null ? task.getPriority() : request.priority());
        task.setMilestone(milestone);
        task.setAssignee(assignee);
        task.setDependsOn(dependsOn);
        task.setDueDate(request.dueDate());
        task.setBlockerNote(request.blockerNote());
        Task saved = tasks.save(task);

        if (assignee != null && !assignee.getId().equals(manager.getId())) {
            notifier.notify(assignee, NotificationService.Type.TASK_ASSIGNED, "Task updated",
                    manager.getFullName() + " updated \"" + task.getTitle() + "\" — check the new details.",
                    "/tasks?project=" + task.getProject().getId());
        }
        return TaskView.from(saved);
    }

    @Transactional
    public TaskService.TaskView cancel(User manager, Long taskId) {
        Task task = load(taskId);
        access.requireManager(manager, task.getProject().getId());
        if (task.getStatus() != Task.Status.CANCELLED) {
            task.setStatus(Task.Status.CANCELLED);
            tasks.save(task);
            if (task.getAssignee() != null && !task.getAssignee().getId().equals(manager.getId())) {
                notifier.notify(task.getAssignee(), NotificationService.Type.TASK_REVIEWED, "Task cancelled",
                        "\"" + task.getTitle() + "\" was cancelled by " + manager.getFullName() + ".", "/tasks");
            }
        }
        return TaskView.from(task);
    }

    // ---------- status flow (assignee) ----------

    @Transactional
    public TaskView updateStatus(User actor, Long taskId, CommonDtos.TaskStatusRequest request) {
        Task task = load(taskId);
        Project project = task.getProject();
        boolean isManager = access.isOwner(actor, project.getId())
                || access.membership(actor, project.getId())
                        .map(pm -> pm.getRole() == ProjectMember.Role.MANAGER).orElse(false);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(actor.getId());
        if (!isAssignee && !isManager && request.status() != Task.Status.CANCELLED) {
            throw new ForbiddenException("Only the assignee (or a manager) can update this task.");
        }

        Task.Status current = task.getStatus();
        Task.Status next = request.status();
        validateTransition(current, next, request.blockerNote());

        if (next == Task.Status.READY_FOR_REVIEW) {
            if (task.getAssignee() == null) {
                throw new BadRequestException("Assign the task to someone before submitting work for review.");
            }
            task.setBlockerNote(null);
            notifyManagers(project, task.getAssignee(), NotificationService.Type.TASK_REVIEWED,
                    "Work submitted for review",
                    "\"" + task.getTitle() + "\" in \"" + project.getTitle() + "\" is ready for review.",
                    "/tasks?project=" + project.getId());
        }
        if (next == Task.Status.IN_PROGRESS || next == Task.Status.REOPENED) {
            task.setBlockerNote(null);
        }
        if (next == Task.Status.BLOCKED) {
            task.setBlockerNote(request.blockerNote());
            notifyManagers(project, actor, NotificationService.Type.TASK_REVIEWED, "Task blocked",
                    "\"" + task.getTitle() + "\" is blocked: " + (request.blockerNote() == null ? "no details given" : request.blockerNote()),
                    "/tasks?project=" + project.getId());
        }
        task.setStatus(next);
        tasks.save(task);
        return TaskView.from(task);
    }

    private void validateTransition(Task.Status current, Task.Status next, String blockerNote) {
        if (next == Task.Status.BLOCKED && (blockerNote == null || blockerNote.isBlank())) {
            throw new BadRequestException("Describe the blocker when marking a task as blocked.");
        }
        Set<Task.Status> allowed = switch (current) {
            case TODO -> Set.of(Task.Status.IN_PROGRESS, Task.Status.BLOCKED, Task.Status.CANCELLED);
            case IN_PROGRESS -> Set.of(Task.Status.READY_FOR_REVIEW, Task.Status.BLOCKED, Task.Status.CANCELLED);
            case BLOCKED -> Set.of(Task.Status.IN_PROGRESS, Task.Status.CANCELLED);
            case READY_FOR_REVIEW -> Set.of(Task.Status.CANCELLED); // decision comes via review endpoint
            case APPROVED -> Set.of(Task.Status.READY_FOR_REVIEW, Task.Status.CANCELLED); // minor rework before project close
            case REOPENED -> Set.of(Task.Status.IN_PROGRESS, Task.Status.BLOCKED, Task.Status.CANCELLED);
            case CANCELLED -> Set.of();
        };
        if (!allowed.contains(next) && next != current) {
            throw new BadRequestException("Cannot move task from " + current + " to " + next + ".");
        }
    }

    // ---------- review decision (manager) ----------

    @Transactional
    public TaskView review(User reviewer, Long taskId, CommonDtos.ReviewRequest request) {
        Task task = load(taskId);
        access.requireManager(reviewer, task.getProject().getId());
        if (task.getStatus() != Task.Status.READY_FOR_REVIEW) {
            throw new BadRequestException("Only tasks submitted for review can be reviewed.");
        }
        if (task.getAssignee() != null && task.getAssignee().getId().equals(reviewer.getId())) {
            throw new BadRequestException("You cannot review your own work submission.");
        }
        reviews.save(TaskReview.builder()
                .task(task)
                .reviewer(reviewer)
                .decision(request.decision())
                .comment(request.comment())
                .build());

        if (request.decision() == TaskReview.Decision.APPROVED) {
            task.setStatus(Task.Status.APPROVED);
            task.setCompletedAt(java.time.Instant.now());
        } else {
            task.setStatus(Task.Status.REOPENED);
        }
        tasks.save(task);

        if (task.getAssignee() != null && !task.getAssignee().getId().equals(reviewer.getId())) {
            notifier.notify(task.getAssignee(),
                    request.decision() == TaskReview.Decision.APPROVED
                            ? NotificationService.Type.TASK_REVIEWED : NotificationService.Type.TASK_REVIEWED,
                    request.decision() == TaskReview.Decision.APPROVED ? "Work approved 👏" : "Changes requested",
                    "\"" + task.getTitle() + "\": " + (request.comment() == null
                            ? (request.decision() == TaskReview.Decision.APPROVED ? "approved." : "please make changes.")
                            : request.comment()),
                    "/tasks?project=" + task.getProject().getId());
        }
        return TaskView.from(task);
    }

    @Transactional(readOnly = true)
    public List<CommonDtos.ReviewResponse> taskReviews(Long taskId) {
        return reviews.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(CommonDtos.ReviewResponse::from).toList();
    }

    // ---------- comments ----------

    @Transactional
    public List<CommonDtos.CommentResponse> comments(User requester, Long taskId) {
        Task task = load(taskId);
        access.requireMember(requester, task.getProject().getId());
        return comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommonDtos.CommentResponse::from).toList();
    }

    @Transactional
    public CommonDtos.CommentResponse addComment(User author, Long taskId, String body) {
        Task task = load(taskId);
        access.requireMember(author, task.getProject().getId());
        TaskComment saved = comments.save(TaskComment.builder().task(task).author(author).body(body.trim()).build());
        // notify assignee + creator (skip author)
        Set<User> toNotify = new HashSet<>();
        addIfKnown(toNotify, task.getAssignee());
        addIfKnown(toNotify, task.getCreatedBy());
        for (User u : toNotify) {
            if (!u.getId().equals(author.getId())) {
                notifier.notify(u, NotificationService.Type.COMMENT_ADDED, "New comment on a task",
                        author.getFullName() + " commented on \"" + task.getTitle() + "\": " + truncate(body, 120),
                        "/tasks?project=" + task.getProject().getId());
            }
        }
        return CommonDtos.CommentResponse.from(saved);
    }

    // ---------- assignee suggestions (F7: clear skill-matching rules) ----------

    @Transactional(readOnly = true)
    public List<CommonDtos.AssigneeSuggestion> assigneeSuggestions(User requester, Long projectId) {
        access.requireManager(requester, projectId);
        List<ProjectMember> members = memberRepo.findByProject(loadProjectFor(projectId));
        List<ProjectSkill> required = projectSkills.findByProjectId(projectId);

        List<CommonDtos.AssigneeSuggestion> suggestions = new ArrayList<>();
        for (ProjectMember pm : members) {
            User candidate = pm.getUser();
            Map<Long, UserSkill.Level> skillsOfCandidate = new HashMap<>();
            userSkills.findByUserId(candidate.getId())
                    .forEach(us -> skillsOfCandidate.put(us.getSkill().getId(), us.getLevel()));

            long matches = required.stream().filter(ps -> skillsOfCandidate.containsKey(ps.getSkill().getId())).count();
            long strongMatches = required.stream()
                    .filter(ps -> skillsOfCandidate.containsKey(ps.getSkill().getId()))
                    .filter(ps -> levelScore(skillsOfCandidate.get(ps.getSkill().getId())) >= levelScore(ps.getRequiredLevel()))
                    .count();
            String reason = strongMatches > 0
                    ? "Meets " + strongMatches + " required skill level(s)"
                    : (matches > 0 ? "Partial skill overlap (" + matches + ")" : "New teammate, open to any task");
            suggestions.add(new CommonDtos.AssigneeSuggestion(candidate.getId(), candidate.getFullName(),
                    String.valueOf(matches * 2 + strongMatches), reason));
        }
        suggestions.sort((a, b) -> {
            int byScore = Integer.parseInt(b.matchScore()) - Integer.parseInt(a.matchScore());
            return byScore != 0 ? byScore : a.fullName().compareTo(b.fullName());
        });
        return suggestions;
    }

    private int levelScore(UserSkill.Level level) {
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
            case EXPERT -> 4;
        };
    }

    // ---------- helpers ----------

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) return null;
        ProjectMember pm = memberRepo.findByProjectIdAndUserId(project.getId(), assigneeId)
                .orElseThrow(() -> new BadRequestException("The assignee must already be a member of the project."));
        return pm.getUser();
    }

    private Milestone resolveMilestone(Project project, Long milestoneId) {
        if (milestoneId == null) return null;
        Milestone ms = milestoneRepo.findById(milestoneId)
                .orElseThrow(() -> new BadRequestException("Milestone not found."));
        if (!ms.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("That milestone belongs to a different project.");
        }
        return ms;
    }

    /** Task dependencies must not create circular chains (SRS U4 note). */
    private Task resolveDependency(Task editedTask, Long dependsOnId, Long sameProjectId) {
        if (dependsOnId == null) return null;
        Task parent = tasks.findById(dependsOnId)
                .orElseThrow(() -> new BadRequestException("Dependency task not found."));
        if (!parent.getProject().getId().equals(sameProjectId)) {
            throw new BadRequestException("A dependency must belong to the same project.");
        }
        if (editedTask != null) {
            Task walk = parent;
            int guard = 0;
            while (walk != null && guard++ < 500) {
                if (walk.getId().equals(editedTask.getId())) {
                    throw new BadRequestException("Circular dependency detected: this task is part of its own chain.");
                }
                walk = walk.getDependsOn();
            }
            if (editedTask.getStatus() == Task.Status.APPROVED && parent.getStatus() != Task.Status.APPROVED) {
                throw new BadRequestException("An approved task cannot depend on unapproved work.");
            }
        }
        return parent;
    }

    private void notifyManagers(Project project, User actor, NotificationService.Type type,
                                String title, String body, String link) {
        memberRepo.findByProject(project).stream()
                .filter(pm -> pm.getRole() == ProjectMember.Role.OWNER || pm.getRole() == ProjectMember.Role.MANAGER)
                .map(ProjectMember::getUser)
                .filter(u -> !u.getId().equals(actor.getId()))
                .forEach(u -> notifier.notify(u, type, title, body, link));
    }

    private Task load(Long taskId) {
        return tasks.findById(taskId).orElseThrow(() -> new BadRequestException("Task not found."));
    }

    private Project loadProjectFor(Long projectId) {
        return projects.findById(projectId).orElseThrow(() -> new BadRequestException("Project not found."));
    }

    private static void addIfKnown(Set<User> set, User u) {
        if (u != null) set.add(u);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "…";
    }
}
