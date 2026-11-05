package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.DirectMessage;
import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.Task;
import com.skillbridge.backend.entity.UploadedFile;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.DirectMessageRepository;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import com.skillbridge.backend.repository.TaskRepository;
import com.skillbridge.backend.repository.UploadedFileRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** U6 / F10–F11: project chat, private messages, task threading via comments, file sharing. */
@Service
public class CollaborationService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv",
            "png", "jpg", "jpeg", "gif", "webp", "zip");

    private final ProjectMemberRepository members;
    private final com.skillbridge.backend.repository.ProjectMessageRepository messages;
    private final DirectMessageRepository direct;
    private final UploadedFileRepository files;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final ProjectAccess access;
    private final NotificationService notifier;
    private final Path uploadDir;
    private final long maxBytes;

    public CollaborationService(ProjectMemberRepository members,
                                com.skillbridge.backend.repository.ProjectMessageRepository messages,
                                DirectMessageRepository direct,
                                UploadedFileRepository files, TaskRepository tasks, UserRepository users,
                                ProjectAccess access, NotificationService notifier,
                                @Value("${skillbridge.upload-dir:./uploads}") String uploadDirValue,
                                @Value("${skillbridge.max-file-size:10485760}") long maxBytes) {
        this.members = members;
        this.messages = messages;
        this.direct = direct;
        this.files = files;
        this.tasks = tasks;
        this.users = users;
        this.access = access;
        this.notifier = notifier;
        this.uploadDir = Paths.get(uploadDirValue).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    // ---------- project chat (group) ----------

    @Transactional
    public List<CommonDtos.MessageResponse> projectMessages(User requester, Long projectId) {
        access.requireMember(requester, projectId);
        return messages.findByProjectIdOrderByIdAsc(projectId, org.springframework.data.domain.PageRequest.of(0, 200))
                .stream().map(m -> new CommonDtos.MessageResponse(m.getId(), m.getSender().getId(),
                        m.getSender().getFullName(), m.getBody(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommonDtos.MessageResponse postProjectMessage(User sender, Long projectId, String body) {
        access.requireMember(sender, projectId);
        var saved = messages.save(com.skillbridge.backend.entity.ProjectMessage.builder()
                .project(com.skillbridge.backend.entity.Project.builder().id(projectId).build())
                .sender(sender)
                .body(body.trim())
                .build());
        members.findByProject(com.skillbridge.backend.entity.Project.builder().id(projectId).build())
                .stream().map(ProjectMember::getUser)
                .filter(u -> !u.getId().equals(sender.getId()))
                .forEach(u -> notifier.notify(u, NotificationService.Type.NEW_MESSAGE, "New project message",
                        sender.getFullName() + " posted in the project chat.",
                        "/projects/" + projectId + "?tab=chat"));
        return new CommonDtos.MessageResponse(saved.getId(), sender.getId(), sender.getFullName(),
                saved.getBody(), saved.getCreatedAt());
    }

    // ---------- private messages ----------

    @Transactional
    public List<CommonDtos.ThreadInfo> myThreads(User me) {
        List<CommonDtos.ThreadInfo> threads = new ArrayList<>();
        for (Long partnerId : direct.findRecentPartnerIds(me.getId())) {
            User partner = users.findById(partnerId).orElse(null);
            if (partner == null) continue;
            List<DirectMessage> conversation = direct.findConversation(me.getId(), partnerId);
            long unread = conversation.stream()
                    .filter(dm -> dm.getRecipient().getId().equals(me.getId()) && dm.getReadAt() == null)
                    .count();
            if (conversation.isEmpty()) continue;
            DirectMessage last = conversation.get(conversation.size() - 1);
            threads.add(new CommonDtos.ThreadInfo(partner.getId(), partner.getFullName(),
                    last.getBody(), last.getCreatedAt(), unread));
        }
        return threads;
    }

    @Transactional
    public List<CommonDtos.MessageResponse> conversation(User me, Long partnerId) {
        User partner = users.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("That student was not found."));
        markConversationRead(me, partner);
        return direct.findConversation(me.getId(), partnerId).stream()
                .map(dm -> new CommonDtos.MessageResponse(dm.getId(), dm.getSender().getId(),
                        dm.getSender().getFullName(), dm.getBody(), dm.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommonDtos.MessageResponse sendDirect(User sender, Long recipientId, String body) {
        if (sender.getId().equals(recipientId)) {
            throw new BadRequestException("You cannot message yourself.");
        }
        User recipient = users.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("That student was not found."));
        if (recipient.getStatus() != User.Status.ACTIVE) {
            throw new BadRequestException("This recipient is not available.");
        }
        var saved = direct.save(DirectMessage.builder().sender(sender).recipient(recipient).body(body.trim()).build());
        notifier.notify(recipient, NotificationService.Type.NEW_MESSAGE, "New private message",
                sender.getFullName() + " sent you a message.",
                "/collaboration?dm=" + sender.getId());
        return new CommonDtos.MessageResponse(saved.getId(), sender.getId(), sender.getFullName(),
                saved.getBody(), saved.getCreatedAt());
    }

    private void markConversationRead(User me, User partner) {
        for (DirectMessage dm : direct.findByRecipientIdAndReadAtIsNull(me.getId())) {
            if (dm.getSender().getId().equals(partner.getId())) {
                dm.setReadAt(Instant.now());
            }
            direct.save(dm);
        }
    }

    // ---------- files ----------

    @Transactional
    public List<CommonDtos.FileResponse> projectFiles(User requester, Long projectId) {
        access.requireMember(requester, projectId);
        return files.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(f -> new CommonDtos.FileResponse(f.getId(), f.getProject().getId(),
                        f.getTask() == null ? null : f.getTask().getId(), f.getOriginalName(),
                        f.getMimeType(), f.getSizeBytes(), f.getUploader().getId(),
                        f.getUploader().getFullName(), f.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommonDtos.FileResponse upload(User uploader, Long projectId, Long taskId, MultipartFile multipart) {
        access.requireMember(uploader, projectId);
        if (multipart == null || multipart.isEmpty()) {
            throw new BadRequestException("Choose a file to upload.");
        }
        if (multipart.getSize() > maxBytes) {
            throw new BadRequestException("This file exceeds the supported size limit of "
                    + (maxBytes / (1024 * 1024)) + " MB.");
        }
        String original = multipart.getOriginalFilename() == null ? "file" : multipart.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type: ." + extension
                    + ". Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        Task task = null;
        if (taskId != null) {
            task = tasks.findById(taskId).orElseThrow(() -> new BadRequestException("Linked task not found."));
            if (!task.getProject().getId().equals(projectId)) {
                throw new BadRequestException("The linked task belongs to a different project.");
            }
        }

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new BadRequestException("Upload folder could not be prepared. Please try again.");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            multipart.transferTo(uploadDir.resolve(storedName).toFile());
        } catch (IOException ex) {
            throw new BadRequestException("The upload could not be saved. Please try again.");
        }

        UploadedFile saved = files.save(UploadedFile.builder()
                .project(com.skillbridge.backend.entity.Project.builder().id(projectId).build())
                .uploader(uploader)
                .task(task)
                .originalName(original)
                .storedName(storedName)
                .mimeType(multipart.getContentType())
                .sizeBytes(multipart.getSize())
                .build());

        notifyOtherMembers(projectId, uploader, NotificationService.Type.FILE_UPLOADED, "New file shared",
                uploader.getFullName() + " uploaded \"" + original + "\".",
                "/projects/" + projectId + "?tab=files");
        return new CommonDtos.FileResponse(saved.getId(), projectId, taskId, original,
                saved.getMimeType(), saved.getSizeBytes(), uploader.getId(),
                uploader.getFullName(), saved.getCreatedAt());
    }

    @Transactional
    public java.nio.file.Path downloadPath(User requester, Long fileId) {
        UploadedFile f = files.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        access.requireMember(requester, f.getProject().getId()); // only approved members download project files
        Path path = uploadDir.resolve(f.getStoredName());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("The stored file is missing on the server.");
        }
        return path;
    }

    @Transactional
    public UploadedFile meta(Long fileId) {
        return files.findById(fileId).orElseThrow(() -> new ResourceNotFoundException("File not found."));
    }

    @Transactional
    public void deleteFile(User requester, Long fileId) {
        UploadedFile f = meta(fileId);
        boolean _isUploader = f.getUploader().getId().equals(requester.getId());
        boolean _isManager = access.isOwner(requester, f.getProject().getId())
                || access.membership(requester, f.getProject().getId())
                        .map(pm -> pm.getRole() == ProjectMember.Role.MANAGER).orElse(false);
        if (!_isUploader && !_isManager) {
            throw new ForbiddenException("Only the uploader or a project manager can remove this file.");
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(f.getStoredName()));
        } catch (IOException ignored) {
            // keep going; the DB record should not block a member's removal attempt
        }
        files.delete(f);
    }

    // ---------- helpers ----------

    private void notifyOtherMembers(Long projectId, User actor, NotificationService.Type type, String title, String body, String link) {
        members.findByProject(com.skillbridge.backend.entity.Project.builder().id(projectId).build())
                .stream().map(ProjectMember::getUser)
                .filter(u -> !u.getId().equals(actor.getId()))
                .forEach(u -> notifier.notify(u, type, title, body, link));
    }

    private static String extensionOf(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) return "";
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
