package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.ProfileDtos;
import com.skillbridge.backend.entity.StudentProfile;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.entity.UserSkill;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.exception.ForbiddenException;
import com.skillbridge.backend.exception.ResourceNotFoundException;
import com.skillbridge.backend.repository.SkillRepository;
import com.skillbridge.backend.repository.StudentProfileRepository;
import com.skillbridge.backend.repository.UserRepository;
import com.skillbridge.backend.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** U1 / F2: profile create/update, skills list management. */
@Service
public class ProfileService {

    private final StudentProfileRepository profileRepo;
    private final UserSkillRepository userSkillsRepo;
    private final SkillRepository skillRepo;
    private final UserRepository userRepo;

    public ProfileService(StudentProfileRepository profileRepo, UserSkillRepository userSkillsRepo,
                          SkillRepository skillRepo, UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.userSkillsRepo = userSkillsRepo;
        this.skillRepo = skillRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ProfileDtos.ProfileResponse myProfile(User me) {
        return view(me.getId(), me);
    }

    @Transactional
    public ProfileDtos.ProfileResponse view(Long userId, User requester) {
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        StudentProfile profile = profileRepo.findByUserId(userId)
                .orElseGet(() -> StudentProfile.builder().user(owner).build());
        boolean allowedSelf = requester != null && requester.getId().equals(userId);
        boolean allowedAdmin = requester != null && requester.getRole() == User.Role.ADMIN;
        if (profile.getVisibility() == StudentProfile.Visibility.PRIVATE && !allowedSelf && !allowedAdmin) {
            throw new ForbiddenException("This profile is private.");
        }
        var skills = userSkillsRepo.findByUserId(userId).stream().map(ProfileDtos.SkillTile::from).toList();
        return new ProfileDtos.ProfileResponse(userId, owner.getFullName(), owner.getEmail(), profile, skills);
    }

    @Transactional
    public ProfileDtos.ProfileResponse update(User me, ProfileDtos.ProfileUpdateRequest request) {
        StudentProfile profile = profileRepo.findByUserId(me.getId()).orElse(null);
        if (profile == null) {
            profile = StudentProfile.builder().user(me).build();
        }
        profile.setCollege(request.college());
        profile.setCourse(request.course());
        profile.setYearOfStudy(request.yearOfStudy());
        profile.setBio(request.bio());
        profile.setInterests(request.interests());
        profile.setHoursPerWeek(request.hoursPerWeek() == null ? StudentProfile.HoursPerWeek.SIX_10 : request.hoursPerWeek());
        profile.setVisibility(request.visibility() == null ? StudentProfile.Visibility.PUBLIC : request.visibility());
        profile.setGithubUrl(request.githubUrl());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setPortfolioUrl(request.portfolioUrl());
        profileRepo.save(profile);
        return myProfile(me);
    }

    @Transactional
    public ProfileDtos.ProfileResponse addSkill(User me, Long skillId, UserSkill.Level level) {
        var skill = skillRepo.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found."));
        var existing = userSkillsRepo.findByUserId(me.getId()).stream()
                .filter(us -> us.getSkill().getId().equals(skillId))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setLevel(level == null ? UserSkill.Level.BEGINNER : level);
            userSkillsRepo.save(existing);
        } else {
            userSkillsRepo.save(UserSkill.builder()
                    .user(me)
                    .skill(skill)
                    .level(level == null ? UserSkill.Level.BEGINNER : level)
                    .build());
        }
        return myProfile(me);
    }

    @Transactional
    public ProfileDtos.ProfileResponse removeSkill(User me, Long skillId) {
        userSkillsRepo.deleteByUserIdAndSkillId(me.getId(), skillId);
        return myProfile(me);
    }

    /** U2 / F5: student directory for teammate search (public profiles only). */
    @Transactional
    public List<ProfileDtos.StudentCard> searchStudents(String q, Long skillId) {
        Map<Long, List<UserSkill>> skillsByUser = new java.util.HashMap<>();
        List<UserSkill> all = userSkillsRepo.findAll();
        for (UserSkill us : all) {
            skillsByUser.computeIfAbsent(us.getUser().getId(), k -> new ArrayList<>()).add(us);
        }
        List<ProfileDtos.StudentCard> out = new ArrayList<>();
        for (StudentProfile profile : profileRepo.findByVisibility(StudentProfile.Visibility.PUBLIC)) {
            User owner = profile.getUser();
            if (owner.getStatus() != User.Status.ACTIVE) continue;
            List<UserSkill> owned = skillsByUser.getOrDefault(owner.getId(), List.of());
            boolean matchesQuery = q == null || q.isBlank()
                    || owner.getFullName().toLowerCase().contains(q.toLowerCase())
                    || owner.getEmail().toLowerCase().contains(q.toLowerCase())
                    || (profile.getInterests() != null && profile.getInterests().toLowerCase().contains(q.toLowerCase()))
                    // skill-name search: typing "java"/"figma" must find people who HAVE that skill
                    || owned.stream().anyMatch(us -> us.getSkill().getName().toLowerCase().contains(q.toLowerCase()));
            boolean matchesSkill = skillId == null || owned.stream()
                    .anyMatch(us -> us.getSkill().getId().equals(skillId));
            if (!matchesQuery || !matchesSkill) continue;
            List<ProfileDtos.SkillTile> tiles = skillsByUser.getOrDefault(owner.getId(), List.of()).stream()
                    .map(ProfileDtos.SkillTile::from).toList();
            out.add(new ProfileDtos.StudentCard(owner.getId(), owner.getFullName(), owner.getEmail(), profile, tiles));
        }
        out.sort((a, b) -> a.fullName().compareTo(b.fullName()));
        return out;
    }
}
