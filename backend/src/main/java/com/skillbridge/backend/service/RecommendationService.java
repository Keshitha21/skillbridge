package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.dto.ProjectDtos;
import com.skillbridge.backend.entity.*;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.repository.ProjectMemberRepository;
import com.skillbridge.backend.repository.ProjectRepository;
import com.skillbridge.backend.repository.ProjectSkillRepository;
import com.skillbridge.backend.repository.StudentProfileRepository;
import com.skillbridge.backend.repository.UserRepository;
import com.skillbridge.backend.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * U2 / F5: teammate suggestions with clear skill-matching rules.
 * Score = 2 per matched required skill + 3 per requirement met at level, plus availability and interest hints.
 */
@Service
public class RecommendationService {

    private final UserRepository users;
    private final UserSkillRepository userSkills;
    private final StudentProfileRepository profiles;
    private final ProjectRepository projects;
    private final ProjectSkillRepository projectSkills;
    private final ProjectMemberRepository members;

    public RecommendationService(UserRepository users, UserSkillRepository userSkills,
                                 StudentProfileRepository profiles, ProjectRepository projects,
                                 ProjectSkillRepository projectSkills, ProjectMemberRepository members) {
        this.users = users;
        this.userSkills = userSkills;
        this.profiles = profiles;
        this.projects = projects;
        this.projectSkills = projectSkills;
        this.members = members;
    }

    /** Who should be invited to this project? Owner/manager view. */
    @Transactional(readOnly = true)
    public List<CommonDtos.AssigneeSuggestion> teammateSuggestions(Long projectId) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found."));
        List<ProjectSkill> required = projectSkills.findByProjectId(projectId);
        List<Long> memberIds = members.findByProject(project).stream().map(pm -> pm.getUser().getId()).toList();

        suggestions(required, memberIds, null);
        List<CommonDtos.AssigneeSuggestion> out = new java.util.ArrayList<>();
        for (User candidate : users.findAll()) {
            if (candidate.getRole() != User.Role.STUDENT || candidate.getStatus() != User.Status.ACTIVE) continue;
            if (memberIds.contains(candidate.getId())) continue;
            var profileOpt = profiles.findByUserId(candidate.getId());
            StudentProfile profile = profileOpt.orElse(null);

            Map<Long, UserSkill.Level> skills = new HashMap<>();
            userSkills.findByUserId(candidate.getId()).forEach(us -> skills.put(us.getSkill().getId(), us.getLevel()));

            int matched = 0, atLevel = 0;
            StringBuilder reasons = new StringBuilder();
            for (ProjectSkill ps : required) {
                UserSkill.Level have = skills.get(ps.getSkill().getId());
                if (have == null) {
                    continue;
                }
                matched++;
                if (score(have) >= score(ps.getRequiredLevel())) atLevel++;
                if (reasons.length() > 0) reasons.append(", ");
                reasons.append(ps.getSkill().getName());
            }
            int scorePoints = matched * 2 + atLevel * 3;
            if (profile != null) {
                switch (profile.getHoursPerWeek()) {
                    case TWENTY_PLUS, ELEVEN_20 -> scorePoints += 2;
                    case SIX_10 -> scorePoints += 1;
                    default -> { }
                }
                if (reasons.length() == 0 && profile.getInterests() != null && !profile.getInterests().isBlank()) {
                    reasons.append("interested in ").append(abbreviate(profile.getInterests(), 40));
                }
            }
            if (reasons.length() > 0) reasons.insert(0, "matches ");
            String reason = reasons.length() > 0
                    ? reasons + (profile != null ? " · " + availabilityLabel(profile.getHoursPerWeek()) + " hrs/week" : "")
                    : "Open teammate, no direct skill overlap yet";
            out.add(new CommonDtos.AssigneeSuggestion(candidate.getId(), candidate.getFullName(),
                    String.valueOf(scorePoints), reason));
        }
        out.sort((a, b) -> Integer.parseInt(b.matchScore()) - Integer.parseInt(a.matchScore()));
        return out.stream().limit(12).toList();
    }

    private List<CommonDtos.AssigneeSuggestion> suggestions(List<ProjectSkill> required, List<Long> memberIds, Object ignore) {
        return List.of(); // reserved hook for future weighting, no-op for now
    }

    /** Which published projects suit me? Discovery page suggestions. */
    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectSummary> projectSuggestions(User me) {
        Set<Long> mySkillIds = new HashSet<>();
        userSkills.findByUserId(me.getId()).forEach(us -> mySkillIds.add(us.getSkill().getId()));

        var published = projects.findAll((root, query, cb) -> cb.equal(root.get("status"), Project.Status.PUBLISHED));
        Map<Long, Integer> scores = new HashMap<>();
        for (Project p : published) {
            int score = 0;
            for (ProjectSkill ps : projectSkills.findByProjectId(p.getId())) {
                if (mySkillIds.contains(ps.getSkill().getId())) score += 2;
            }
            scores.put(p.getId(), score);
        }
        return published.stream()
                .sorted((a, b) -> Integer.compare(scores.getOrDefault(b.getId(), 0), scores.getOrDefault(a.getId(), 0)))
                .limit(9)
                .map(p -> {
                    int score = scores.getOrDefault(p.getId(), 0);
                    boolean myProject = members.findByProjectIdAndUserId(p.getId(), me.getId()).isPresent();
                    Long projectId = p.getId();
                    var summary = new ProjectDtos.ProjectSummary(p.getId(), p.getTitle(), p.getDescription(),
                            p.getCategory(), p.getStatus(), p.getOwner().getFullName(), p.getOwner().getId(),
                            members.findByProject(p).size(), p.getTeamSizeMax(), p.getTargetDeadline(),
                            p.getCreatedAt(),
                            projectSkills.findByProjectId(p.getId()).stream().map(x -> x.getSkill().getName()).toList(),
                            myProject ? "MEMBER" : null);
                    return score > 0 || myProject ? summary : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static int score(UserSkill.Level level) {
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
            case EXPERT -> 4;
        };
    }

    private static String availabilityLabel(StudentProfile.HoursPerWeek h) {
        return switch (h) {
            case ONE_5 -> "1–5";
            case SIX_10 -> "6–10";
            case ELEVEN_20 -> "11–20";
            case TWENTY_PLUS -> "20+";
        };
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "…";
    }
}
