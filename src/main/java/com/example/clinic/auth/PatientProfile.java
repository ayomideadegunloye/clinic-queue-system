package com.example.clinic.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Minimal identity for the front desk. Never holds clinical information. userId is null for walk-ins. */
@Entity
@Table(name = "patient_profile")
@Getter @Setter @NoArgsConstructor
public class PatientProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    private String contact;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
