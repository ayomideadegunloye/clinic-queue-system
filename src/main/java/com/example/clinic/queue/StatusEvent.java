package com.example.clinic.queue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit trail: who moved which visit from which status to which, and when. */
@Entity
@Table(name = "status_event")
@Getter @Setter @NoArgsConstructor
public class StatusEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "queue_entry_id", nullable = false)
    private UUID queueEntryId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status")
    private VisitStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false)
    private VisitStatus toStatus;
    @Column(name = "actor_user_id")
    private UUID actorUserId;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    public static StatusEvent of(UUID entryId, VisitStatus from, VisitStatus to, UUID actor, Instant at) {
        StatusEvent e = new StatusEvent();
        e.queueEntryId = entryId;
        e.fromStatus = from;
        e.toStatus = to;
        e.actorUserId = actor;
        e.changedAt = at;
        return e;
    }
}
