package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("SELECT m FROM DirectMessage m WHERE (m.sender.id = :a AND m.recipient.id = :b) OR (m.sender.id = :b AND m.recipient.id = :a) ORDER BY m.createdAt ASC")
    List<DirectMessage> findConversation(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT CASE WHEN m.sender.id = :me THEN m.recipient.id ELSE m.sender.id END AS partner, MAX(m.createdAt) " +
           "FROM DirectMessage m WHERE m.sender.id = :me OR m.recipient.id = :me GROUP BY partner ORDER BY MAX(m.createdAt) DESC")
    List<Long> findRecentPartnerIds(@Param("me") Long me);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    List<DirectMessage> findByRecipientIdAndReadAtIsNull(Long recipientId);
}
