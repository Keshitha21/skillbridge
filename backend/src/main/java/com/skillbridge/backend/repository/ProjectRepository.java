package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    List<Project> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<Project> findByStatusOrderByIdDesc(Project.Status status);
    List<Project> findByOwnerId(Long ownerId);
}
