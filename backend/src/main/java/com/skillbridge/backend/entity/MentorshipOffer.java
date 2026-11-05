package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "mentorship_offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MentorshipOffer {

    public enum Type { OFFER, REQUEST }
    public enum Status { OPEN, IN_PROGRESS, COMPLETED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    private int maxParticipants = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(insertable=false, updatable=false)
    private Instant createdAt;
}
