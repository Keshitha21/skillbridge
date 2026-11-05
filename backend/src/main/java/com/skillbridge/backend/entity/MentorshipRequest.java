package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "mentorship_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MentorshipRequest {

    public enum Status { PENDING, ACCEPTED, DECLINED, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private MentorshipOffer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(insertable=false, updatable=false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
