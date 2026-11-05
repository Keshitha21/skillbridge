package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, Long> {
    List<ProjectMessage> findByProjectIdOrderByIdAsc(Long projectId, Pageable pageable);
    List<ProjectMessage> findByProjectId(Project project);
}
