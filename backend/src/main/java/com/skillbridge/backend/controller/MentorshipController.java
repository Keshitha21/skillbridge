package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.MentorshipOffer;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.MentorshipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** U8 / F13: peer-learning offers & requests + feedback (F13 tail). */
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
}
