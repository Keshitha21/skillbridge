package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRoleRepository extends JpaRepository<ProjectRole, Long> {
    List<ProjectRole> findByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
