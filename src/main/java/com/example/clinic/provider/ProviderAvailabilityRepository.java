package com.example.clinic.provider;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, UUID> {
    List<ProviderAvailability> findByProviderIdOrderByWeekdayAscStartTimeAsc(UUID providerId);
    List<ProviderAvailability> findByProviderIdAndWeekday(UUID providerId, int weekday);
    void deleteByProviderId(UUID providerId);
}
