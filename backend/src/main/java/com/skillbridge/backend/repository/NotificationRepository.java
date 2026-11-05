package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByIdDesc(Long userId);

    List<Notification> findByUserIdAndReadFlagFalse(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    List<Notification> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.readFlag = true WHERE n.user.id = :userId")
    void markAllRead(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

    void deleteByCreatedAtBefore(Instant cutoff);
}
