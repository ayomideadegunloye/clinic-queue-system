package com.example.clinic.provider;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One working interval on an ISO weekday (Mon=1). Several rows per day model breaks. */
@Entity
@Table(name = "provider_availability")
@Getter @Setter @NoArgsConstructor
public class ProviderAvailability {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    private int weekday;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes;
}
