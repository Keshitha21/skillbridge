package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.TaskReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {
    List<TaskReview> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskReview> findByTaskIdIn(List<Long> taskIds);
}
