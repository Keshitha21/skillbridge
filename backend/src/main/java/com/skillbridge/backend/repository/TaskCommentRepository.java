package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    List<TaskComment> findByTaskIdIn(List<Long> ids);
}
