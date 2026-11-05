package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.dto.ProfileDtos;
import com.skillbridge.backend.entity.StudentProfile;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.entity.UserSkill;
import com.skillbridge.backend.service.ProfileService;
import com.skillbridge.backend.service.SkillService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** U1 / F2: profile view, update, skill tiles + skill catalogue endpoints. */
@RestController
public class ProfileController {

    public record ProfilePayload(
            String college, String course, Integer yearOfStudy, String bio, String interests,
            StudentProfile.HoursPerWeek hoursPerWeek, StudentProfile.Visibility visibility,
            String githubUrl, String linkedinUrl, String portfolioUrl) {}

    public record SkillPayload(@NotNull Long skillId, UserSkill.Level level) {}

    private final ProfileService profiles;
    private final SkillService skills;

    public ProfileController(ProfileService profiles, SkillService skills) {
        this.profiles = profiles;
        this.skills = skills;
    }

    @GetMapping("/api/profile")
    public ResponseEntity<ProfileDtos.ProfileResponse> mine(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(profiles.myProfile(me));
    }

    @PutMapping("/api/profile")
    public ResponseEntity<ProfileDtos.ProfileResponse> update(@AuthenticationPrincipal User me,
                                                              @RequestBody ProfilePayload payload) {
        return ResponseEntity.ok(profiles.update(me, new ProfileDtos.ProfileUpdateRequest(
                payload.college(), payload.course(), payload.yearOfStudy(), payload.bio(), payload.interests(),
                payload.hoursPerWeek(), payload.visibility(), payload.githubUrl(), payload.linkedinUrl(),
                payload.portfolioUrl())));
    }

    @GetMapping("/api/students/{userId}")
    public ResponseEntity<ProfileDtos.ProfileResponse> other(@AuthenticationPrincipal User me,
                                                             @PathVariable Long userId) {
        return ResponseEntity.ok(profiles.view(userId, me));
    }

    @GetMapping("/api/students")
    public ResponseEntity<List<ProfileDtos.StudentCard>> search(@RequestParam(required = false) String q,
                                                                @RequestParam(required = false) Long skillId) {
        return ResponseEntity.ok(profiles.searchStudents(q, skillId));
    }

    @PostMapping("/api/profile/skills")
    public ResponseEntity<ProfileDtos.ProfileResponse> addSkill(@AuthenticationPrincipal User me,
                                                                @RequestBody SkillPayload payload) {
        return ResponseEntity.ok(profiles.addSkill(me, payload.skillId(), payload.level()));
    }

    @DeleteMapping("/api/profile/skills/{skillId}")
    public ResponseEntity<ProfileDtos.ProfileResponse> removeSkill(@AuthenticationPrincipal User me,
                                                                   @PathVariable Long skillId) {
        return ResponseEntity.ok(profiles.removeSkill(me, skillId));
    }

    @GetMapping("/api/skills")
    public ResponseEntity<List<CommonDtos.SkillDto>> allSkills() {
        return ResponseEntity.ok(skills.allSkills());
    }

    @GetMapping("/api/skill-categories")
    public ResponseEntity<List<CommonDtos.SkillCategoryDto>> categories() {
        return ResponseEntity.ok(skills.categories());
    }
}
