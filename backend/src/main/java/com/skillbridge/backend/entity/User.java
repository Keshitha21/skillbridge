package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    public enum Role { STUDENT, ADMIN }
    public enum Status { ACTIVE, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String fullName;

    @Column(nullable=false, unique=true, length=180)
    private String email;

    @Column(nullable=false, length=120)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    @Builder.Default
    private Role role = Role.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(insertable=false, updatable=false)
    private Instant createdAt;
}
