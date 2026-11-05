package com.skillbridge.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A teaching file uploaded inside a mentorship activity (notes, slides, exercises…).
 *  Bytes live in the same uploads/ directory as project files; this table holds the metadata. */
@Entity
@Table(name = "mentorship_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MentorshipFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private MentorshipOffer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, unique = true, length = 255)
    private String storedName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
