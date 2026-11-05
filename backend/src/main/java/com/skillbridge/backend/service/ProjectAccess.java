package com.skillbridge.backend.service;

import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Central membership / permission checks for project workspaces (U3–U6, U9). */
@Component
public class ProjectAccess {

    private final ProjectMemberRepository members;

    public ProjectAccess(ProjectMemberRepository members) {
        this.members = members;
    }

    public Optional<ProjectMember> membership(User requester, Long projectId) {
        return members.findByProjectIdAndUserId(projectId, requester.getId());
    }

    public boolean canAccess(User requester, Long projectId) {
        if (requester.getRole() == User.Role.ADMIN) return true;
        return membership(requester, projectId).isPresent();
    }

    /** any approved member (or admin) — private workspace gate */
    public void requireMember(User requester, Long projectId) {
        if (!canAccess(requester, projectId)) {
            throw new ForbiddenException("Only approved project members can access this workspace.");
        }
    }

    /** owner, manager or admin — manage team / tasks gate */
    public void requireManager(User requester, Long projectId) {
        if (requester.getRole() == User.Role.ADMIN) return;
        ProjectMember pm = membership(requester, projectId)
                .orElseThrow(() -> new ForbiddenException("Only project owners or managers can do this."));
        if (pm.getRole() != ProjectMember.Role.OWNER && pm.getRole() != ProjectMember.Role.MANAGER) {
            throw new ForbiddenException("Only project owners or managers can do this.");
        }
    }

    public boolean isOwner(User requester, Long projectId) {
        if (requester.getRole() == User.Role.ADMIN) return true;
        return membership(requester, projectId)
                .map(pm -> pm.getRole() == ProjectMember.Role.OWNER)
                .orElse(false);
    }

    /** strict owner gate (status changes, deletion, promote) */
    public void requireOwner(User requester, Long projectId) {
        if (!isOwner(requester, projectId)) {
            throw new ForbiddenException("Only the project owner can do this.");
        }
    }
}
