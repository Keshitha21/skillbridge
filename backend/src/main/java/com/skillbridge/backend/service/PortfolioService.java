package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.PortfolioItem;
import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.PortfolioItemRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** U8 / F14: portfolio of completed projects and achievements with visibility settings. */
@Service
public class PortfolioService {

    private final PortfolioItemRepository repo;
    private final ProjectRepository projects;

    public PortfolioService(PortfolioItemRepository repo, ProjectRepository projects) {
        this.repo = repo;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<CommonDtos.PortfolioItemResponse> mine(User me) {
        return repo.findByUserIdOrderByCreatedAtDesc(me.getId()).stream().map(this::toResponse).toList();
    }

    /** Public view honours visibility settings; admins can see private entries too. */
    @Transactional
    public List<CommonDtos.PortfolioItemResponse> forUser(Long userId, User requester) {
        boolean self = requester != null && requester.getId().equals(userId);
        boolean admin = requester != null && requester.getRole() == User.Role.ADMIN;
        if (self || admin) {
            return repo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
        }
        return repo.findByUserIdAndVisibility(userId, PortfolioItem.Visibility.PUBLIC).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CommonDtos.PortfolioItemResponse create(User me, CommonDtos.CreatePortfolioItem request) {
        Project linked = request.projectId() == null ? null : projects.findById(request.projectId()).orElse(null);
        PortfolioItem saved = repo.save(PortfolioItem.builder()
                .user(me)
                .project(linked)
                .title(request.title().trim())
                .description(request.description())
                .tag(request.tag() == null ? PortfolioItem.Tag.PROJECT : request.tag())
                .link(request.link())
                .visibility(request.visibility() == null ? PortfolioItem.Visibility.PUBLIC : request.visibility())
                .build());
        return toResponse(saved);
    }

    @Transactional
    public CommonDtos.PortfolioItemResponse update(User me, Long itemId, CommonDtos.CreatePortfolioItem request) {
        PortfolioItem item = repo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found."));
        if (!item.getUser().getId().equals(me.getId())) {
            throw new ForbiddenException("You can only edit your own portfolio entries.");
        }
        item.setTitle(request.title().trim());
        item.setDescription(request.description());
        item.setProject(request.projectId() == null ? null : projects.findById(request.projectId()).orElse(null));
        item.setTag(request.tag() == null ? item.getTag() : request.tag());
        item.setLink(request.link());
        item.setVisibility(request.visibility() == null ? item.getVisibility() : request.visibility());
        return toResponse(repo.save(item));
    }

    @Transactional
    public void delete(User me, Long itemId) {
        PortfolioItem item = repo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found."));
        if (!item.getUser().getId().equals(me.getId()) && me.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("You can only delete your own portfolio entries.");
        }
        repo.delete(item);
    }

    private CommonDtos.PortfolioItemResponse toResponse(PortfolioItem item) {
        return new CommonDtos.PortfolioItemResponse(item.getId(), item.getUser().getId(),
                item.getUser().getFullName(),
                item.getProject() == null ? null : item.getProject().getId(),
                item.getProject() == null ? null : item.getProject().getTitle(),
                item.getTitle(), item.getDescription(), item.getTag(), item.getLink(),
                item.getVisibility(), item.getCreatedAt());
    }
}
