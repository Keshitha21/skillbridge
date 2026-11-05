package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByOfferId(Long offerId);

    List<Feedback> findByToUserId(Long userId);

    List<Feedback> findByFromUserId(Long userId);

    boolean existsByOfferIdAndFromUserId(Long offerId, Long fromUserId);

    List<Feedback> findByOfferIdIn(List<Long> offerIds);
}
