package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssigneeIdOrderByUpdatedAtDesc(Long assigneeId);

    List<Task> findByAssigneeIdAndProjectId(Long assigneeId, Long projectId);

    List<Task> findByProjectIdIn(List<Long> projectIds);
}
