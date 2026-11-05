package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.dto.ProjectDtos;
import com.skillbridge.backend.entity.Milestone;
import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.ProjectAccess;
import com.skillbridge.backend.service.ProjectService;
import com.skillbridge.backend.service.RequestService;
import com.skillbridge.backend.service.TaskService;
import com.skillbridge.backend.repository.MilestoneRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** U3 / F3–F6 + U5 progress and milestones. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    public record MilestonePayload(String title, String description, java.time.LocalDate targetDate,
                                   Milestone.Status status) {}

    private final ProjectService projects;
    private final ProjectAccess access;
    private final RequestService requests;
    private final com.skillbridge.backend.service.RecommendationService recommendationService;
    private final MilestoneRepository milestones;

    public ProjectController(ProjectService projects, ProjectAccess access, RequestService requests,
                             com.skillbridge.backend.service.RecommendationService recommendationService,
                             MilestoneRepository milestones) {
        this.projects = projects;
        this.access = access;
        this.requests = requests;
        this.recommendationService = recommendationService;
        this.milestones = milestones;
    }

    // ---------- discovery (U2 / F3) ----------
    @GetMapping
    public ResponseEntity<List<ProjectDtos.ProjectSummary>> search(
            @AuthenticationPrincipal User me,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Project.Status status,
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @RequestParam(required = false, defaultValue = "false") boolean owned) {
        return ResponseEntity.ok(projects.search(me, q, category, skillId, status, mine, owned));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDtos.ProjectDetail> detail(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(projects.detail(me, id));
    }

    // ---------- lifecycle (U3 / F4) ----------
    @PostMapping
    public ResponseEntity<ProjectDtos.ProjectRef> create(@AuthenticationPrincipal User me,
                                                         @Valid @RequestBody ProjectDtos.UpsertProjectRequest request) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.create(me, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDtos.ProjectRef> update(@AuthenticationPrincipal User me, @PathVariable Long id,
                                                         @Valid @RequestBody ProjectDtos.UpsertProjectRequest request) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.update(me, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonDtos.OkResponse> delete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        projects.delete(me, id);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Project cancelled and removed."));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ProjectDtos.ProjectRef> publish(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.setStatus(me, id, Project.Status.PUBLISHED)));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ProjectDtos.ProjectRef> pause(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.setStatus(me, id, Project.Status.PAUSED)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ProjectDtos.ProjectRef> complete(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.setStatus(me, id, Project.Status.COMPLETED)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ProjectDtos.ProjectRef> cancel(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(ProjectDtos.ProjectRef.from(projects.setStatus(me, id, Project.Status.CANCELLED)));
    }

    // ---------- members ----------
    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectDtos.MemberResponse>> members(@AuthenticationPrincipal User me,
                                                                    @PathVariable Long id) {
        return ResponseEntity.ok(projects.listMembers(me, id));
    }

    @PostMapping("/{id}/members/{userId}/promote")
    public ResponseEntity<CommonDtos.OkResponse> promote(@AuthenticationPrincipal User me, @PathVariable Long id,
                                                         @PathVariable Long userId) {
        projects.promoteMember(me, id, userId);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Member promoted to manager."));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<CommonDtos.OkResponse> remove(@AuthenticationPrincipal User me, @PathVariable Long id,
                                                        @PathVariable Long userId) {
        projects.removeMember(me, id, userId);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Member removed from the team."));
    }

    // ---------- join & invitations (U3 / F6) ----------
    @PostMapping("/{id}/join-requests")
    public ResponseEntity<CommonDtos.JoinRequestResponse> askToJoin(@AuthenticationPrincipal User me,
                                                                    @PathVariable Long id,
                                                                    @RequestBody(required = false)
                                                                    CommonDtos.JoinRequestCreate body) {
        return ResponseEntity.ok(requests.askToJoin(me, id, body == null ? null : body.message()));
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<CommonDtos.JoinRequestResponse> invite(@AuthenticationPrincipal User me,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody CommonDtos.InviteCreate body) {
        return ResponseEntity.ok(requests.invite(me, id, body.userId(), body.message()));
    }

    @PostMapping("/{id}/teammate-suggestions")
    public ResponseEntity<List<CommonDtos.AssigneeSuggestion>> teammateSuggestions(
            @AuthenticationPrincipal User me, @PathVariable Long id) {
        access.requireManager(me, id);
        return ResponseEntity.ok(recommendationService.teammateSuggestions(id));
    }

    // ---------- members of a project can read the requests list on their detail screen ----------
    @GetMapping("/{id}/requests")
    public ResponseEntity<List<CommonDtos.JoinRequestResponse>> projectRequests(@AuthenticationPrincipal User me,
                                                                                @PathVariable Long id) {
        access.requireManager(me, id);
        return ResponseEntity.ok(requests.myRequests(me).stream()
                .filter(r -> r.projectId().equals(id) && "PENDING".equals(r.status().name()))
                .toList());
    }

    // ---------- progress & milestones (U5 / F9) ----------
    @GetMapping("/{id}/progress")
    public ResponseEntity<ProjectDtos.ProgressReport> progress(@AuthenticationPrincipal User me,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(projects.progress(me, id));
    }

    @GetMapping("/{id}/milestones")
    public ResponseEntity<List<ProjectDtos.MilestoneDto>> listMilestones(@AuthenticationPrincipal User me,
                                                                         @PathVariable Long id) {
        access.requireMember(me, id);
        return ResponseEntity.ok(milestones.findByProjectIdOrderByTargetDateAsc(id).stream()
                .map(ProjectDtos.MilestoneDto::from).toList());
    }

    @PostMapping("/{id}/milestones")
    public ResponseEntity<ProjectDtos.MilestoneDto> addMilestone(@AuthenticationPrincipal User me,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody MilestonePayload payload) {
        access.requireManager(me, id);
        Milestone ms = milestones.save(Milestone.builder()
                .project(com.skillbridge.backend.entity.Project.builder().id(id).build())
                .title(payload.title())
                .description(payload.description())
                .targetDate(payload.targetDate())
                .status(payload.status() == null ? Milestone.Status.PLANNED : payload.status())
                .build());
        return ResponseEntity.ok(ProjectDtos.MilestoneDto.from(ms));
    }

    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<ProjectDtos.MilestoneDto> updateMilestone(@AuthenticationPrincipal User me,
                                                                    @PathVariable Long milestoneId,
                                                                    @Valid @RequestBody MilestonePayload payload) {
        Milestone ms = milestones.findById(milestoneId)
                .orElseThrow(() -> new com.skillbridge.backend.exception.ResourceNotFoundException("Milestone not found."));
        access.requireManager(me, ms.getProject().getId());
        ms.setTitle(payload.title());
        ms.setDescription(payload.description());
        ms.setTargetDate(payload.targetDate());
        ms.setStatus(payload.status() == null ? ms.getStatus() : payload.status());
        milestones.save(ms);
        return ResponseEntity.ok(ProjectDtos.MilestoneDto.from(ms));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<CommonDtos.OkResponse> deleteMilestone(@AuthenticationPrincipal User me,
                                                                 @PathVariable Long milestoneId) {
        Milestone ms = milestones.findById(milestoneId)
                .orElseThrow(() -> new com.skillbridge.backend.exception.ResourceNotFoundException("Milestone not found."));
        access.requireManager(me, ms.getProject().getId());
        milestones.delete(ms);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Milestone deleted."));
    }
}
