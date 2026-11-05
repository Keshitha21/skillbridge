package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.ProjectDtos;
import com.skillbridge.backend.entity.*;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.JoinRequestRepository;
import com.skillbridge.backend.repository.MilestoneRepository;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.ProjectRoleRepository;
import com.skillbridge.backend.repository.ProjectSkillRepository;
import com.skillbridge.backend.repository.SkillRepository;
import com.skillbridge.backend.repository.TaskRepository;
import com.skillbridge.backend.repository.spec.ProjectSpecs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** U3 / F4 (create, edit, publish, pause, cancel) and U5 / F9 (progress). */
@Service
public class ProjectService {

    private static final List<Task.Status> LIVE_TASK_STATUSES = List.of(
            Task.Status.TODO, Task.Status.IN_PROGRESS, Task.Status.BLOCKED,
            Task.Status.READY_FOR_REVIEW, Task.Status.REOPENED);

    private final ProjectRepository projects;
    private final ProjectSkillRepository projectSkills;
    private final ProjectMemberRepository members;
    private final ProjectRoleRepository roles;
    private final JoinRequestRepository joinRequests;
    private final MilestoneRepository milestones;
    private final TaskRepository tasksRepo;
    private final SkillRepository skillRepo;
    private final ProjectAccess access;
    private final NotificationService notifier;

    public ProjectService(ProjectRepository projects, ProjectSkillRepository projectSkills,
                          ProjectMemberRepository members, ProjectRoleRepository roles,
                          JoinRequestRepository joinRequests, MilestoneRepository milestones,
                          TaskRepository tasksRepo, SkillRepository skillRepo,
                          ProjectAccess access, NotificationService notifier) {
        this.projects = projects;
        this.projectSkills = projectSkills;
        this.members = members;
        this.roles = roles;
        this.joinRequests = joinRequests;
        this.milestones = milestones;
        this.tasksRepo = tasksRepo;
        this.skillRepo = skillRepo;
        this.access = access;
        this.notifier = notifier;
    }

    @Transactional
    public Project create(User me, ProjectDtos.UpsertProjectRequest request) {
        Project project = Project.builder()
                .owner(me)
                .title(request.title().trim())
                .description(request.description())
                .goals(request.goals())
                .category(request.category())
                .status(request.status() == Project.Status.PUBLISHED ? Project.Status.PUBLISHED : Project.Status.DRAFT)
                .teamSizeMax(request.teamSizeMax() == null ? 5 : request.teamSizeMax())
                .startDate(request.startDate())
                .targetDeadline(request.targetDeadline())
                .build();
        Project saved = projects.save(project);

        members.save(ProjectMember.builder().project(saved).user(me).role(ProjectMember.Role.OWNER).build());

        applySkills(saved, request.skills());
        applyRoles(saved, request.roles());
        return saved;
    }

    @Transactional
    public Project update(User me, Long projectId, ProjectDtos.UpsertProjectRequest request) {
        access.requireOwner(me, projectId);
        Project project = load(projectId);
        project.setTitle(request.title().trim());
        project.setDescription(request.description());
        project.setGoals(request.goals());
        project.setCategory(request.category());
        if (request.teamSizeMax() != null && request.teamSizeMax() > 0) {
            project.setTeamSizeMax(request.teamSizeMax());
        }
        project.setStartDate(request.startDate());
        project.setTargetDeadline(request.targetDeadline());
        applySkills(project, request.skills());
        applyRoles(project, request.roles());

        notifyMembers(project, me, "Project updated",
                me.getFullName() + " updated the details of \"" + project.getTitle() + "\".",
                "/projects/" + projectId);
        return projects.save(project);
    }

    /** U3 / F4: owner-controlled lifecycle changes. */
    @Transactional
    public Project setStatus(User me, Long projectId, Project.Status status) {
        Project project = load(projectId);
        switch (status) {
            case PUBLISHED, PAUSED, CANCELLED -> access.requireOwner(me, projectId);
            case COMPLETED -> access.requireManager(me, projectId);
            case DRAFT -> throw new BadRequestException("A published project cannot go back to draft; edit and publish again.");
        }
        if (project.getStatus() == Project.Status.CANCELLED && status != Project.Status.CANCELLED) {
            throw new BadRequestException("A cancelled project cannot be reopened.");
        }
        project.setStatus(status);
        notifyMembers(project, me, "Project update",
                "\"" + project.getTitle() + "\" is now " + status.name() + ".", "/projects/" + projectId);
        return projects.save(project);
    }

    @Transactional
    public void delete(User me, Long projectId) {
        access.requireOwner(me, projectId);
        projects.delete(load(projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectSummary> search(User requester, String q, String category, Long skillId,
                                                   Project.Status status, boolean mine, boolean ownedByMe) {
        List<Project> found = projects.findAll(ProjectSpecs.filter(q, category, skillId, status, requester, mine, ownedByMe));
        return found.stream().map(p -> toSummary(p, requester)).collect(Collectors.toList());
    }

    @Transactional
    public ProjectDtos.ProjectDetail detail(User requester, Long projectId) {
        Project project = load(projectId);
        boolean requesterIsMember = access.canAccess(requester, projectId);
        ProjectDtos.ProjectSummary summary = toSummary(project, requester);
        List<ProjectDtos.SkillLine> skills = projectSkills.findByProjectId(projectId).stream()
                .map(ps -> new ProjectDtos.SkillLine(ps.getSkill().getId(), ps.getSkill().getName(), ps.getRequiredLevel()))
                .toList();

        if (!requesterIsMember) {
            if (project.getStatus() != Project.Status.PUBLISHED) {
                throw new ResourceNotFoundException("Project is not available.");
            }
            // public preview: goals, member list and workspace data stay private
            return new ProjectDtos.ProjectDetail(summary, null, List.of(), skills, openRoles(project),
                    List.of(), null, List.of());
        }

        List<ProjectDtos.MemberResponse> memberList = members.findByProjectOrderByRoleAsc(project).stream()
                .map(ProjectDtos.MemberResponse::from)
                .toList();

        boolean canManage = access.isOwner(requester, projectId) || isManager(requester, projectId);
        List<ProjectDtos.RequestLine> pending = canManage
                ? joinRequests.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .filter(jr -> jr.getStatus() == JoinRequest.Status.PENDING && jr.getType() == JoinRequest.Type.JOIN_REQUEST)
                        .map(ProjectDtos.RequestLine::from)
                        .toList()
                : List.of();

        return new ProjectDtos.ProjectDetail(summary, project.getGoals(), memberList, skills, openRoles(project),
                milestones.findByProjectIdOrderByTargetDateAsc(projectId).stream()
                        .map(ProjectDtos.MilestoneDto::from).toList(),
                progress(requester, projectId),
                pending);
    }

    private boolean isManager(User requester, Long projectId) {
        return access.membership(requester, projectId)
                .map(pm -> pm.getRole() == ProjectMember.Role.MANAGER)
                .orElse(false);
    }

    @Transactional
    public List<ProjectDtos.OpenRole> openRoles(Project project) {
        List<ProjectMember> projectMembers = members.findByProject(project);
        List<ProjectDtos.OpenRole> open = new ArrayList<>();
        for (ProjectRole role : roles.findByProjectId(project.getId())) {
            long filled = projectMembers.stream()
                    .filter(pm -> pm.getProjectRole() != null && pm.getProjectRole().getId().equals(role.getId()))
                    .count();
            if (role.getSlots() - filled > 0) {
                open.add(new ProjectDtos.OpenRole(role.getId(), role.getTitle(), role.getSlots(), (int) filled));
            }
        }
        return open;
    }

    @Transactional
    public ProjectDtos.ProjectSummary toSummary(Project p, User requester) {
        Optional<ProjectMember> membership = access.membership(requester, p.getId());
        return new ProjectDtos.ProjectSummary(
                p.getId(), p.getTitle(), p.getDescription(), p.getCategory(), p.getStatus(),
                p.getOwner().getFullName(), p.getOwner().getId(),
                members.findByProject(p).size(),
                p.getTeamSizeMax(), p.getTargetDeadline(), p.getCreatedAt(),
                projectSkills.findByProjectId(p.getId()).stream().map(ps -> ps.getSkill().getName()).toList(),
                membership.map(pm -> pm.getRole().name()).orElse(null));
    }

    /** U5 / F9: milestones, status counts, upcoming deadlines, overdue, blockers, pending reviews. */
    @Transactional
    public ProjectDtos.ProgressReport progress(User requester, Long projectId) {
        access.requireMember(requester, projectId);
        List<Task> all = tasksRepo.findByProjectId(projectId);
        long total = all.size();
        long cancelled = all.stream().filter(t -> t.getStatus() == Task.Status.CANCELLED).count();
        long todo = count(all, Task.Status.TODO);
        long inProgress = count(all, Task.Status.IN_PROGRESS);
        long blocked = count(all, Task.Status.BLOCKED);
        long ready = count(all, Task.Status.READY_FOR_REVIEW);
        long approved = count(all, Task.Status.APPROVED);
        long reopened = count(all, Task.Status.REOPENED);

        LocalDate today = LocalDate.now();
        long eligible = total - cancelled;
        long overdue = all.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> LIVE_TASK_STATUSES.contains(t.getStatus()))
                .filter(t -> t.getDueDate().isBefore(today))
                .count();

        int percent = eligible == 0 ? 0 : (int) Math.round(approved * 100.0 / eligible);

        List<ProjectDtos.UpcomingDeadline> deadlines = all.stream()
                .filter(t -> LIVE_TASK_STATUSES.contains(t.getStatus()) && t.getDueDate() != null)
                .sorted(Comparator.comparing(Task::getDueDate).thenComparing(Task::getId))
                .limit(8)
                .map(t -> new ProjectDtos.UpcomingDeadline(t.getId(), t.getTitle(),
                        describeDue(t.getDueDate(), today)))
                .toList();

        List<ProjectDtos.MilestoneProgress> milestonesProgress = milestones.findByProjectIdOrderByTargetDateAsc(projectId)
                .stream()
                .map(ms -> {
                    List<Task> msTasks = all.stream()
                            .filter(t -> t.getMilestone() != null && t.getMilestone().getId().equals(ms.getId()))
                            .toList();
                    long msTotal = msTasks.stream().filter(t -> t.getStatus() != Task.Status.CANCELLED).count();
                    long msDone = msTasks.stream().filter(t -> t.getStatus() == Task.Status.APPROVED).count();
                    return new ProjectDtos.MilestoneProgress(ms.getId(), ms.getTitle(), ms.getStatus(), msTotal, msDone);
                })
                .toList();

        return new ProjectDtos.ProgressReport(total, todo, inProgress, blocked, ready, approved,
                reopened, cancelled, overdue, ready, percent, deadlines, milestonesProgress);
    }

    @Transactional
    public List<ProjectDtos.MemberResponse> listMembers(User requester, Long projectId) {
        access.requireMember(requester, projectId);
        Project project = load(projectId);
        return members.findByProjectOrderByRoleAsc(project).stream()
                .map(ProjectDtos.MemberResponse::from).toList();
    }

    @Transactional
    public void promoteMember(User requester, Long projectId, Long userId) {
        access.requireOwner(requester, projectId);
        ProjectMember pm = memberOrThrow(projectId, userId);
        if (pm.getRole() == ProjectMember.Role.OWNER) {
            throw new BadRequestException("The project owner role cannot be changed.");
        }
        pm.setRole(ProjectMember.Role.MANAGER);
        members.save(pm);
        notifier.notify(pm.getUser(), NotificationService.Type.PROJECT_UPDATE,
                "You are now a project manager",
                requester.getFullName() + " made you a manager of \"" + load(projectId).getTitle() + "\".",
                "/projects/" + projectId);
    }

    @Transactional
    public void removeMember(User requester, Long projectId, Long userId) {
        access.requireManager(requester, projectId);
        ProjectMember pm = memberOrThrow(projectId, userId);
        if (pm.getRole() == ProjectMember.Role.OWNER) {
            throw new BadRequestException("The project owner cannot be removed here.");
        }
        members.delete(pm);
        notifier.notify(pm.getUser(), NotificationService.Type.MEMBER_REMOVED, "Removed from project",
                requester.getFullName() + " removed you from the project team.", "/projects");
    }

    // ---------- internal helpers ----------

    @Transactional
    public void applySkills(Project project, List<ProjectDtos.SkillRef> skillRefs) {
        projectSkills.deleteByProjectId(project.getId());
        if (skillRefs == null) return;
        for (ProjectDtos.SkillRef ref : skillRefs) {
            Skill skill = skillRepo.findById(ref.skillId())
                    .orElseThrow(() -> new BadRequestException("Unknown skill selected: id " + ref.skillId()));
            projectSkills.save(ProjectSkill.builder()
                    .project(project)
                    .skill(skill)
                    .requiredLevel(ref.requiredLevel() == null ? UserSkill.Level.BEGINNER : ref.requiredLevel())
                    .build());
        }
    }

    private void applyRoles(Project project, List<String> roleTitles) {
        if (roleTitles == null || roleTitles.isEmpty()) return;
        for (String title : roleTitles) {
            if (title != null && !title.isBlank()) {
                roles.save(ProjectRole.builder().project(project).title(title.trim())
                        .slots(1).build());
            }
        }
    }

    @Transactional
    public Project load(Long projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }

    private long count(List<Task> all, Task.Status status) {
        return all.stream().filter(t -> t.getStatus() == status).count();
    }

    private ProjectMember memberOrThrow(Long projectId, Long userId) {
        return members.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("That user is not part of this project."));
    }

    private void notifyMembers(Project project, User actor, String title, String body, String link) {
        List<ProjectMember> memberList = members.findByProject(project);
        List<User> others = memberList.stream().map(ProjectMember::getUser)
                .filter(u -> !u.getId().equals(actor.getId())).toList();
        notifier.notifyAll(others, NotificationService.Type.PROJECT_UPDATE, title, body, link);
    }

    private String describeDue(LocalDate due, LocalDate today) {
        if (due.isBefore(today)) {
            return "Overdue by " + ChronoUnit.DAYS.between(due, today) + " day(s)";
        }
        if (due.isEqual(today)) return "Due today";
        return "Due in " + ChronoUnit.DAYS.between(today, due) + " day(s) (" + due + ")";
    }
}
