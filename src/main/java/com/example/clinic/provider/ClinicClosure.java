package com.example.clinic.provider;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Full-day closure when startTime/endTime are null, otherwise a partial closure. */
@Entity
@Table(name = "clinic_closure")
@Getter @Setter @NoArgsConstructor
public class ClinicClosure {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "closure_date", nullable = false)
    private LocalDate date;
    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "end_time")
    private LocalTime endTime;
    @Column(name = "reason_label", nullable = false)
    private String reasonLabel;

    public boolean isFullDay() { return startTime == null || endTime == null; }
}
