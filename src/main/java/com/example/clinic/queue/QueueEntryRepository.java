package com.example.clinic.queue;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {
    Optional<QueueEntry> findByAppointmentId(UUID appointmentId);
    List<QueueEntry> findByQueueDate(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QueueEntry q where q.id = :id")
    Optional<QueueEntry> findForUpdate(@Param("id") UUID id);
}
