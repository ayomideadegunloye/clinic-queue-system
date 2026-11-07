package com.example.clinic.provider;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    List<Provider> findByActiveTrueOrderByDisplayNameAsc();
    List<Provider> findAllByOrderByDisplayNameAsc();

    /** Row lock serialises concurrent bookings for the same provider. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Provider p where p.id = :id")
    Optional<Provider> findForUpdate(@Param("id") UUID id);
}
