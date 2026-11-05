package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.MentorshipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorshipRequestRepository extends JpaRepository<MentorshipRequest, Long> {

    List<MentorshipRequest> findByOfferId(Long offerId);

    List<MentorshipRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<MentorshipRequest> findByOfferIdIn(List<Long> offerIds);


    List<MentorshipRequest> findByOffer_OwnerId(Long ownerId);
}
