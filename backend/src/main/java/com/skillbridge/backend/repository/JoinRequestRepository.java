package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<JoinRequest> findByProjectIdInOrderByCreatedAtDesc(List<Long> projectIds);

    List<JoinRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<JoinRequest> findByRequesterIdIn(List<Long> ids);

    Optional<JoinRequest> findByRequesterIdAndProjectIdAndTypeAndStatus(Long requesterId, Long projectId, JoinRequest.Type type, JoinRequest.Status status);

    boolean existsByProjectIdAndRequesterId(Long projectId, Long requesterId);
}
