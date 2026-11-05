package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.ProjectDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** U2 / F5: skill-based suggestions shown on the discovery screens. */
@RestController
public class RecommendationController {

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping("/api/recommendations/projects")
    public ResponseEntity<List<ProjectDtos.ProjectSummary>> forMe(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(recommendations.projectSuggestions(me));
    }
}
