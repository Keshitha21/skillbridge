package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    public enum Status { DRAFT, PUBLISHED, PAUSED, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "team_size_max", nullable = false)
    @Builder.Default
    private Integer teamSizeMax = 5;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "target_deadline")
    private LocalDate targetDeadline;

    @Column(insertable=false, updatable=false)
    private Instant createdAt;

    @Column(insertable=false, updatable=false)
    private Instant updatedAt;
}
