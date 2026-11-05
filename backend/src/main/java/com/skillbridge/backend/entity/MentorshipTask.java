package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A learning-plan task inside a mentorship activity (U8/F13 workspace). Duplicate of the
 *  project task flow, kept separate so mentorship opens/changes never touch project boards. */
@Entity
@Table(name = "mentorship_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MentorshipTask {

    public enum Status { TODO, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private MentorshipOffer offer;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.TODO;

    /** Which participant this learning step belongs to (null = shared/team step). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
