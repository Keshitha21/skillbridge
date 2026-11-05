package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Project;
import com.skillbridge.backend.entity.ProjectMember;
import com.skillbridge.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectOrderByRoleAsc(Project project);

    List<ProjectMember> findByUserId(Long userId);

    List<ProjectMember> findByUser(User user);

    List<ProjectMember> findByProject(Project project);

    List<ProjectMember> findByProjectIdIn(List<Long> projectIds);

    long countByProjectIdAndRole(Long projectId, ProjectMember.Role role);
}
