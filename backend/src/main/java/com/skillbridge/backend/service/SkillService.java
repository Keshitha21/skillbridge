package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.repository.SkillCategoryRepository;
import com.skillbridge.backend.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Public skill catalogue used by profile, project creation and filters. */
@Service
public class SkillService {

    private final SkillRepository skills;
    private final SkillCategoryRepository categories;

    public SkillService(SkillRepository skills, SkillCategoryRepository categories) {
        this.skills = skills;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CommonDtos.SkillCategoryDto> categories() {
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> new CommonDtos.SkillCategoryDto(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommonDtos.SkillDto> allSkills() {
        return skills.findAllByOrderByNameAsc().stream().map(CommonDtos.SkillDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CommonDtos.SkillDto>> skillsByCategory() {
        return allSkills().stream().collect(Collectors.groupingBy(CommonDtos.SkillDto::categoryId));
    }
}
