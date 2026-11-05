package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.ProjectSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectSkillRepository extends JpaRepository<ProjectSkill, Long> {
    List<ProjectSkill> findByProjectId(Long projectId);
    List<ProjectSkill> findByProjectIdIn(List<Long> ids);
    List<ProjectSkill> findBySkillIdIn(List<Long> ids);
    void deleteByProjectId(Long projectId);
}
