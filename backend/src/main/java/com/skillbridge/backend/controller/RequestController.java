package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** U3 / F6: joining, invitations and manage decisions (accept / reject / cancel). */
@RestController
public class RequestController {

    private final RequestService requests;

    public RequestController(RequestService requests) {
        this.requests = requests;
    }

    @GetMapping("/api/requests")
    public ResponseEntity<List<CommonDtos.JoinRequestResponse>> mine(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(requests.myRequests(me));
    }

    @PostMapping("/api/requests/{id}/accept")
    public ResponseEntity<CommonDtos.OkResponse> accept(@AuthenticationPrincipal User me, @PathVariable Long id) {
        requests.decide(me, id, true, false);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Request accepted."));
    }

    @PostMapping("/api/requests/{id}/reject")
    public ResponseEntity<CommonDtos.OkResponse> reject(@AuthenticationPrincipal User me, @PathVariable Long id) {
        requests.decide(me, id, false, false);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Request declined."));
    }

    @PostMapping("/api/requests/{id}/cancel")
    public ResponseEntity<CommonDtos.OkResponse> cancel(@AuthenticationPrincipal User me, @PathVariable Long id) {
        requests.decide(me, id, false, true);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Request cancelled."));
    }
}
