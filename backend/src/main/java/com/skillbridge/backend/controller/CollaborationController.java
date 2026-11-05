package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.CollaborationService;
import com.skillbridge.backend.service.ReportService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** U6 / F10–F11: project chat, private messages, file sharing + reporting into the admin queue. */
@RestController
public class CollaborationController {

    public record MessagePayload(@NotBlank @Size(max = 1000) String body) {}

    private final CollaborationService collab;
    private final ReportService reportService;

    public CollaborationController(CollaborationService collab, ReportService reportService) {
        this.collab = collab;
        this.reportService = reportService;
    }

    // ---------- project group chat ----------
    @GetMapping("/api/projects/{projectId}/messages")
    public ResponseEntity<List<CommonDtos.MessageResponse>> chat(@AuthenticationPrincipal User me, @PathVariable Long projectId) {
        return ResponseEntity.ok(collab.projectMessages(me, projectId));
    }

    @PostMapping("/api/projects/{projectId}/messages")
    public ResponseEntity<CommonDtos.MessageResponse> post(@AuthenticationPrincipal User me, @PathVariable Long projectId,
                                                           @jakarta.validation.Valid @RequestBody MessagePayload payload) {
        return ResponseEntity.ok(collab.postProjectMessage(me, projectId, payload.body()));
    }

    // ---------- private messages ----------
    @GetMapping("/api/messages/threads")
    public ResponseEntity<List<CommonDtos.ThreadInfo>> threads(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(collab.myThreads(me));
    }

    @GetMapping("/api/messages/with/{userId}")
    public ResponseEntity<List<CommonDtos.MessageResponse>> direct(@AuthenticationPrincipal User me,
                                                                   @PathVariable Long userId) {
        return ResponseEntity.ok(collab.conversation(me, userId));
    }

    @PostMapping("/api/messages/with/{userId}")
    public ResponseEntity<CommonDtos.MessageResponse> sendDirect(@AuthenticationPrincipal User me,
                                                                 @PathVariable Long userId,
                                                                 @jakarta.validation.Valid @RequestBody MessagePayload payload) {
        return ResponseEntity.ok(collab.sendDirect(me, userId, payload.body()));
    }

    // ---------- files ----------
    @GetMapping("/api/projects/{projectId}/files")
    public ResponseEntity<List<CommonDtos.FileResponse>> files(@AuthenticationPrincipal User me,
                                                               @PathVariable Long projectId) {
        return ResponseEntity.ok(collab.projectFiles(me, projectId));
    }

    @PostMapping("/api/projects/{projectId}/files")
    public ResponseEntity<CommonDtos.FileResponse> upload(@AuthenticationPrincipal User me, @PathVariable Long projectId,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "taskId", required = false) Long taskId) {
        return ResponseEntity.ok(collab.upload(me, projectId, taskId, file));
    }

    @GetMapping("/api/files/{fileId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User me, @PathVariable Long fileId) {
        Path path = collab.downloadPath(me, fileId);
        try {
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + collab.meta(fileId).getOriginalName() + "\"")
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/api/files/{fileId}")
    public ResponseEntity<CommonDtos.OkResponse> delete(@AuthenticationPrincipal User me, @PathVariable Long fileId) {
        collab.deleteFile(me, fileId);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "File removed."));
    }

    // ---------- report user or content (admin reviews later, U9) ----------
    @PostMapping("/api/reports")
    public ResponseEntity<CommonDtos.OkResponse> report(@AuthenticationPrincipal User me,
                                                        @RequestBody Map<String, Object> payload) {
        reportService.create(me, payload);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true,
                "Report submitted. An administrator will review it."));
    }
}
