package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skill_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    private String description;
}
