package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_skills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_level", nullable = false, length = 15)
    @Builder.Default
    private UserSkill.Level requiredLevel = UserSkill.Level.BEGINNER;
}
