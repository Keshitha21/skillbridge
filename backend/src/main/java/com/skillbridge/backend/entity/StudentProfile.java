package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "student_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentProfile {

    public enum HoursPerWeek { ONE_5, SIX_10, ELEVEN_20, TWENTY_PLUS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    private String college;

    private String course;

    private Integer yearOfStudy;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 500)
    private String interests;

    @Enumerated(EnumType.STRING)
    @Column(name = "hours_per_week", nullable = false, length = 20)
    @Builder.Default
    private HoursPerWeek hoursPerWeek = HoursPerWeek.SIX_10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    @Column(insertable=false, updatable=false)
    private Instant createdAt;

    @Column(insertable=false, updatable=false)
    private Instant updatedAt;

    public enum Visibility { PUBLIC, PRIVATE }
}
