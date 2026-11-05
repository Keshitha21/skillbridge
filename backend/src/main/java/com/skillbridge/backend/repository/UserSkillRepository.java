package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserId(Long userId);
    List<UserSkill> findByUserIdIn(Collection<Long> userIds);
    List<UserSkill> findByUserIdInAndSkillIdIn(Collection<Long> userIds, Collection<Long> skillIds);
    void deleteByUserIdAndSkillId(Long userId, Long skillId);
}
