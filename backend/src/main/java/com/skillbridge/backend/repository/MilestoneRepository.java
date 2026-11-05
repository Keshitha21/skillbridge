package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProjectIdOrderByTargetDateAsc(Long projectId);
    void deleteByProjectId(Long projectId);
}
