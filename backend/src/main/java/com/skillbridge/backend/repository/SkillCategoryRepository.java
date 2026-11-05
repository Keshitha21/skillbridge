package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {
    List<SkillCategory> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
