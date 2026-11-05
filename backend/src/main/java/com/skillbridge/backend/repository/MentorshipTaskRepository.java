package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.MentorshipTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorshipTaskRepository extends JpaRepository<MentorshipTask, Long> {
    List<MentorshipTask> findByOfferIdOrderByCreatedAtAsc(Long offerId);
}
