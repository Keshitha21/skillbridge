package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.MentorshipFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorshipFileRepository extends JpaRepository<MentorshipFile, Long> {
    List<MentorshipFile> findByOfferIdOrderByCreatedAtDesc(Long offerId);
}
