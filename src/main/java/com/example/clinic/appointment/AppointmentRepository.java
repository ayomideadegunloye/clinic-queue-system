package com.example.clinic.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /** Active = not cancelled. Cancelled bookings never block a slot. */
    @Query("""
           select a from Appointment a
           where a.providerId = :providerId
             and a.status <> com.example.clinic.queue.VisitStatus.CANCELLED
             and a.startAt < :to and a.endAt > :from
           """)
    List<Appointment> findActive(@Param("providerId") UUID providerId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    List<Appointment> findByPatientIdOrderByStartAtDesc(UUID patientId);
}
