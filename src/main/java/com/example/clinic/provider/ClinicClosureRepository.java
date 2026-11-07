package com.example.clinic.provider;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicClosureRepository extends JpaRepository<ClinicClosure, UUID> {
    List<ClinicClosure> findByDate(LocalDate date);
    List<ClinicClosure> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);
}
