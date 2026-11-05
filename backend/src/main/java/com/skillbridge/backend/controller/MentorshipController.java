package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.MentorshipFile;
import com.skillbridge.backend.entity.MentorshipOffer;
import com.skillbridge.backend.entity.MentorshipTask;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.MentorshipService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/** U8 / F13: peer-learning offers & requests + feedback + mentorship workspace
 *  (teaching files & learning-plan tasks). */
@RestController
public class MentorshipController {

    public record DecisionPayload(String note) {}

    private final MentorshipService mentorship;

    public MentorshipController(MentorshipService mentorship) {
        this.mentorship = mentorship;
    }

    @GetMapping("/api/mentorship")
    public ResponseEntity<List<CommonDtos.OfferResponse>> search(
            @AuthenticationPrincipal User me,
            @RequestParam(required = false) MentorshipOffer.Type type,
            @RequestParam(required = false) MentorshipOffer.Status status,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(mentorship.search(me, type, status, skillId, q));
    }

    @GetMapping("/api/mentorship/{id}")
    public ResponseEntity<CommonDtos.OfferResponse> one(@AuthenticationPrincipal User me, @PathVariable Long id) {
        return ResponseEntity.ok(mentorship.getOne(id));
    }

    @PostMapping("/api/mentorship")
    public ResponseEntity<CommonDtos.OfferResponse> create(@AuthenticationPrincipal User me,
                                                           @Valid @RequestBody CommonDtos.CreateOffer request) {
        return ResponseEntity.ok(mentorship.create(me, request));
    }

    @PostMapping("/api/mentorship/{id}/close")
    public ResponseEntity<CommonDtos.OkResponse> close(@AuthenticationPrincipal User me, @PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        MentorshipOffer.Status status = null;
        if (body != null && body.get("status") != null) {
            status = MentorshipOffer.Status.valueOf(String.valueOf(body.get("status")));
        }
        mentorship.close(me, id, status);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Learning activity updated."));
    }

    @PostMapping("/api/mentorship/{id}/requests")
    public ResponseEntity<CommonDtos.MentorshipRequestResponse> request(@AuthenticationPrincipal User me,
                                                                        @PathVariable Long id,
                                                                        @RequestBody CommonDtos.CreateMentorshipRequest body) {
        return ResponseEntity.ok(mentorship.request(me, id, body.message()));
    }

    @GetMapping("/api/mentorship/requests")
    public ResponseEntity<List<CommonDtos.MentorshipRequestResponse>> myRequests(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(mentorship.myRequests(me));
    }

    @PostMapping("/api/mentorship/requests/{requestId}/accept")
    public ResponseEntity<CommonDtos.MentorshipRequestResponse> accept(@AuthenticationPrincipal User me,
                                                                       @PathVariable Long requestId) {
        return ResponseEntity.ok(mentorship.decide(me, requestId, true));
    }

    @PostMapping("/api/mentorship/requests/{requestId}/decline")
    public ResponseEntity<CommonDtos.MentorshipRequestResponse> decline(@AuthenticationPrincipal User me,
                                                                        @PathVariable Long requestId) {
        return ResponseEntity.ok(mentorship.decide(me, requestId, false));
    }

    @PostMapping("/api/mentorship/requests/{requestId}/complete")
    public ResponseEntity<CommonDtos.MentorshipRequestResponse> complete(@AuthenticationPrincipal User me,
                                                                         @PathVariable Long requestId) {
        return ResponseEntity.ok(mentorship.complete(me, requestId));
    }

    @PostMapping("/api/mentorship/{offerId}/feedback")
    public ResponseEntity<CommonDtos.FeedbackResponse> feedback(@AuthenticationPrincipal User me,
                                                                @PathVariable Long offerId,
                                                                @Valid @RequestBody CommonDtos.CreateFeedback request) {
        return ResponseEntity.ok(mentorship.leaveFeedback(me, offerId, request.rating(), request.comment()));
    }

    @GetMapping("/api/mentorship/{offerId}/feedback")
    public ResponseEntity<List<CommonDtos.FeedbackResponse>> feedbackForOffer(@AuthenticationPrincipal User me,
                                                                              @PathVariable Long offerId) {
        return ResponseEntity.ok(mentorship.feedbackForOffer(offerId));
    }

    @GetMapping("/api/feedback/received")
    public ResponseEntity<List<CommonDtos.FeedbackResponse>> feedbackReceived(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(mentorship.feedbackFor(me.getId()));
    }

    // ---------- workspace: teaching files ----------

    @GetMapping("/api/mentorship/{offerId}/files")
    public ResponseEntity<List<CommonDtos.MentorshipFileView>> files(@AuthenticationPrincipal User me,
                                                                     @PathVariable Long offerId) {
        return ResponseEntity.ok(mentorship.listFiles(me, offerId));
    }

    @PostMapping(value = "/api/mentorship/{offerId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonDtos.MentorshipFileView> upload(@AuthenticationPrincipal User me,
                                                                @PathVariable Long offerId,
                                                                @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mentorship.uploadFile(me, offerId, file));
    }

    @GetMapping("/api/mentorship/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@AuthenticationPrincipal User me, @PathVariable Long fileId) {
        Path path = mentorship.downloadPath(me, fileId);
        MentorshipFile meta = mentorship.fileMeta(fileId);
        try {
            Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + meta.getOriginalName() + "\"")
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/api/mentorship/files/{fileId}")
    public ResponseEntity<CommonDtos.OkResponse> deleteFile(@AuthenticationPrincipal User me, @PathVariable Long fileId) {
        mentorship.deleteFile(me, fileId);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "File removed."));
    }

    // ---------- workspace: learning-plan tasks ----------

    @GetMapping("/api/mentorship/{offerId}/tasks")
    public ResponseEntity<List<CommonDtos.MentorshipTaskView>> tasks(@AuthenticationPrincipal User me,
                                                                     @PathVariable Long offerId) {
        return ResponseEntity.ok(mentorship.listTasks(offerId));
    }

    @PostMapping("/api/mentorship/{offerId}/tasks")
    public ResponseEntity<CommonDtos.MentorshipTaskView> createTask(@AuthenticationPrincipal User me,
                                                                    @PathVariable Long offerId,
                                                                    @Valid @RequestBody CommonDtos.CreateMentorshipTask body) {
        return ResponseEntity.ok(mentorship.createTask(me, offerId, body));
    }

    @PostMapping("/api/mentorship/tasks/{taskId}/status")
    public ResponseEntity<CommonDtos.MentorshipTaskView> taskStatus(@AuthenticationPrincipal User me,
                                                                    @PathVariable Long taskId,
                                                                    @Valid @RequestBody CommonDtos.MentorshipTaskStatus body) {
        return ResponseEntity.ok(mentorship.updateTaskStatus(me, taskId, body.status()));
    }
}
