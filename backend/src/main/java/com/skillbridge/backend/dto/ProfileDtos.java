package com.skillbridge.backend.dto;

import com.skillbridge.backend.entity.Skill;
import com.skillbridge.backend.entity.StudentProfile;
import com.skillbridge.backend.entity.UserSkill;
import jakarta.validation.constraints.Size;

/** Profile endpoints F2. */
public final class ProfileDtos {

    public record ProfileUpdateRequest(
            @Size(max = 120) String college,
            @Size(max = 120) String course,
            Integer yearOfStudy,
            @Size(max = 2000) String bio,
            @Size(max = 500) String interests,
            StudentProfile.HoursPerWeek hoursPerWeek,
            StudentProfile.Visibility visibility,
            @Size(max = 300) String githubUrl,
            @Size(max = 300) String linkedinUrl,
            @Size(max = 300) String portfolioUrl) {}

    public record ProfileResponse(Long userId, String fullName, String email,
                                  StudentProfile profile, java.util.List<SkillTile> skills) {}

    public record StudentCard(Long userId, String fullName, String email, StudentProfile profile,
                              java.util.List<SkillTile> skills) {}

    public record SkillTile(Long skillId, String skillName, String category, UserSkill.Level level) {
        public static SkillTile from(UserSkill us) {
            Skill skill = us.getSkill();
            return new SkillTile(skill.getId(), skill.getName(),
                    skill.getCategory() == null ? null : skill.getCategory().getName(), us.getLevel());
        }
    }
}
