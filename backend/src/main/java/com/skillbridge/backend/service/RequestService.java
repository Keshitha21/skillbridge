package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.JoinRequest;
import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.repository.JoinRequestRepository;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** U3 / F6: join requests and invitations with accept / reject / cancel decisions. */
@Service
public class RequestService {

    private final JoinRequestRepository requests;
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final UserRepository users;
    private final ProjectAccess access;
    private final NotificationService notifier;

    public RequestService(JoinRequestRepository requests, ProjectRepository projects,
                          ProjectMemberRepository members, UserRepository users,
                          ProjectAccess access, NotificationService notifier) {
        this.requests = requests;
        this.projects = projects;
        this.members = members;
        this.users = users;
        this.access = access;
        this.notifier = notifier;
    }

    @Transactional
    public CommonDtos.JoinRequestResponse askToJoin(User me, Long projectId, String message) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found."));
        if (project.getStatus() != Project.Status.PUBLISHED) {
            throw new BadRequestException("This project is not accepting new members right now.");
        }
        if (members.findByProjectIdAndUserId(projectId, me.getId()).isPresent()) {
            throw new BadRequestException("You are already a member of this project.");
        }
        if (requests.existsByProjectIdAndRequesterId(projectId, me.getId())) {
            throw new BadRequestException("You already have a pending request or invitation for this project.");
        }
        JoinRequest jr = requests.save(JoinRequest.builder()
                .project(project)
                .requester(me)
                .type(JoinRequest.Type.JOIN_REQUEST)
                .message(message)
                .status(JoinRequest.Status.PENDING)
                .build());
        notifyManagers(project, me,
                "New join request",
                me.getFullName() + " asked to join \"" + project.getTitle() + "\".",
                "/requests");
        return CommonDtos.JoinRequestResponse.from(jr, project.getTitle());
    }

    @Transactional
    public CommonDtos.JoinRequestResponse invite(User manager, Long projectId, Long userId, String message) {
        access.requireManager(manager, projectId);
        Project project = projects.findById(projectId).orElseThrow();
        User invitee = users.findById(userId)
                .orElseThrow(() -> new BadRequestException("That student was not found."));
        if (members.findByProjectIdAndUserId(projectId, invitee.getId()).isPresent()) {
            throw new BadRequestException(invitee.getFullName() + " is already a member of this project.");
        }
        if (requests.existsByProjectIdAndRequesterId(projectId, invitee.getId())) {
            throw new BadRequestException("A pending request or invitation already exists for this student.");
        }
        JoinRequest jr = requests.save(JoinRequest.builder()
                .project(project)
                .requester(invitee)
                .type(JoinRequest.Type.INVITATION)
                .message(message)
                .status(JoinRequest.Status.PENDING)
                .build());
        notifier.notify(invitee, NotificationService.Type.INVITE_RECEIVED, "You were invited to a project",
                manager.getFullName() + " invited you to \"" + project.getTitle() + "\".",
                "/requests");
        return CommonDtos.JoinRequestResponse.from(jr, project.getTitle());
    }

    /** Everything related to the user: outgoing requests/invitations + incoming ones on managed projects. */
    @Transactional
    public List<CommonDtos.JoinRequestResponse> myRequests(User me) {
        List<Long> myManagedProjectIds = members.findByUserId(me.getId()).stream()
                .filter(pm -> pm.getRole() == ProjectMember.Role.OWNER || pm.getRole() == ProjectMember.Role.MANAGER)
                .map(pm -> pm.getProject().getId())
                .toList();
        List<JoinRequest> incoming = myManagedProjectIds.isEmpty()
                ? List.of()
                : requests.findByProjectIdInOrderByCreatedAtDesc(myManagedProjectIds);
        List<JoinRequest> outgoing = requests.findByRequesterIdOrderByCreatedAtDesc(me.getId());

        List<CommonDtos.JoinRequestResponse> result = new ArrayList<>();
        incoming.forEach(jr -> result.add(withDirection(jr, "INCOMING")));
        outgoing.forEach(jr -> result.add(withDirection(jr, "OUTGOING")));
        return result;
    }

    private CommonDtos.JoinRequestResponse withDirection(JoinRequest jr, String direction) {
        CommonDtos.JoinRequestResponse base = CommonDtos.JoinRequestResponse.from(jr, jr.getProject().getTitle());
        // audience fields double as direction markers: JOIN_REQUEST -> owner/manager decides; INVITATION -> invitee decides
        return new CommonDtos.JoinRequestResponse(base.id(), base.projectId(), base.projectTitle(),
                base.requesterId(), base.requesterName(), base.type(), base.status(), base.message(),
                base.createdAt(), base.requesterId(), direction);
    }

    /** Owners/managers decide JOIN_REQUESTs; the invited student decides invitations. */
    @Transactional
    public void decide(User me, Long requestId, boolean accept, boolean cancelOwn) {
        JoinRequest jr = requests.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found."));
        Project project = jr.getProject();
        boolean decidedByManager = jr.getType() == JoinRequest.Type.JOIN_REQUEST &&
                (access.isOwner(me, project.getId()) ||
                        access.membership(me, project.getId())
                                .map(pm -> pm.getRole() == ProjectMember.Role.MANAGER).orElse(false));
        boolean decidedByInvitee = jr.getType() == JoinRequest.Type.INVITATION
                && jr.getRequester().getId().equals(me.getId());
        boolean ownCancel = cancelOwn && jr.getRequester().getId().equals(me.getId());

        if (!decidedByManager && !decidedByInvitee && !ownCancel) {
            throw new ForbiddenException("You are not allowed to decide this request.");
        }
        if (jr.getStatus() != JoinRequest.Status.PENDING) {
            throw new BadRequestException("This request was already handled.");
        }

        if (ownCancel && !decidedByManager && !decidedByInvitee) {
            jr.setStatus(JoinRequest.Status.CANCELLED);
            requests.save(jr);
            return;
        }

        if (accept) {
            long occupied = members.countByProjectIdAndRole(project.getId(), ProjectMember.Role.MEMBER)
                    + members.countByProjectIdAndRole(project.getId(), ProjectMember.Role.MANAGER)
                    + members.countByProjectIdAndRole(project.getId(), ProjectMember.Role.OWNER);
            if (occupied >= project.getTeamSizeMax()) {
                throw new BadRequestException("The project team is already full.");
            }
            jr.setStatus(JoinRequest.Status.ACCEPTED);
            jr.setDecidedBy(me);
            members.save(ProjectMember.builder()
                    .project(project)
                    .user(jr.getRequester())
                    .role(ProjectMember.Role.MEMBER)
                    .build());
            if (jr.getType() == JoinRequest.Type.JOIN_REQUEST) {
                notifier.notify(jr.getRequester(), NotificationService.Type.REQUEST_DECIDED,
                        "Join request accepted",
                        "Your request to join \"" + project.getTitle() + "\" was accepted.",
                        "/projects/" + project.getId());
                notifyManagers(project, jr.getRequester(), "New team member",
                        jr.getRequester().getFullName() + " joined \"" + project.getTitle() + "\".",
                        "/projects/" + project.getId());
            } else {
                notifier.notify(project.getOwner(), NotificationService.Type.INVITE_DECIDED,
                        "Invitation accepted",
                        jr.getRequester().getFullName() + " accepted your invitation to \"" + project.getTitle() + "\".",
                        "/projects/" + project.getId());
            }
        } else {
            jr.setStatus(JoinRequest.Status.REJECTED);
            jr.setDecidedBy(me);
            if (jr.getType() == JoinRequest.Type.JOIN_REQUEST) {
                notifier.notify(jr.getRequester(), NotificationService.Type.REQUEST_DECIDED,
                        "Join request declined",
                        "Your request to join \"" + project.getTitle() + "\" was not accepted.",
                        "/projects");
            }
        }
        requests.save(jr);
    }

    private void notifyManagers(Project project, User actor, String title, String body, String link) {
        List<ProjectMember> projectMembers = members.findByProject(project);
        for (ProjectMember pm : projectMembers) {
            if ((pm.getRole() == ProjectMember.Role.OWNER || pm.getRole() == ProjectMember.Role.MANAGER)
                    && !pm.getUser().getId().equals(actor.getId())) {
                notifier.notify(pm.getUser(), NotificationService.Type.JOIN_REQUEST, title, body, link);
            }
        }
    }
}
