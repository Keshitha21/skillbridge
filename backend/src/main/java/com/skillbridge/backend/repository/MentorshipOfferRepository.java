package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.MentorshipOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorshipOfferRepository extends JpaRepository<MentorshipOffer, Long> {

    List<MentorshipOffer> findByStatusOrderByCreatedAtDesc(MentorshipOffer.Status status);

    List<MentorshipOffer> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<MentorshipOffer> findByTypeAndStatus(MentorshipOffer.Type type, MentorshipOffer.Status status);

    List<MentorshipOffer> findByTypeOrderByCreatedAtDesc(MentorshipOffer.Type type);
}
