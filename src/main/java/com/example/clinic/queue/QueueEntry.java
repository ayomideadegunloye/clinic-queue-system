package com.example.clinic.queue;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One visit in the clinic day. appointmentId is null for walk-ins. */
@Entity
@Table(name = "queue_entry")
@Getter @Setter @NoArgsConstructor
public class QueueEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "appointment_id")
    private UUID appointmentId;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;
    @Column(name = "checked_in_at")
    private Instant checkedInAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private VisitStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}
