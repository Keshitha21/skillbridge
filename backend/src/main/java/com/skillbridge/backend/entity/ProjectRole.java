package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private int slots = 1;
}
