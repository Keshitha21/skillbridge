package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByOrderByNameAsc();
    List<Skill> findByCategoryIdOrderByNameAsc(Long categoryId);
    List<Skill> findByCategoryIn(List<com.skillbridge.backend.entity.SkillCategory> categories);
    Optional<Skill> findByNameIgnoreCase(String name);
}
