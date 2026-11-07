package com.example.clinic.provider;

import com.example.clinic.common.ApiException;
import com.example.clinic.config.ClinicTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {
    public record AvailabilityInput(Integer weekday, LocalTime startTime, LocalTime endTime, Integer slotMinutes) {}
    public record ProviderAdminView(Provider provider, List<ProviderAvailability> hours) {}

    private final ProviderRepository providers;
    private final ProviderAvailabilityRepository availabilities;
    private final ClinicClosureRepository closures;
    private final ClinicTime time;

    public ProviderService(ProviderRepository providers, ProviderAvailabilityRepository availabilities,
                           ClinicClosureRepository closures, ClinicTime time) {
        this.providers = providers;
        this.availabilities = availabilities;
        this.closures = closures;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<Provider> activeProviders() {
        return providers.findByActiveTrueOrderByDisplayNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ProviderAdminView> adminView() {
        return providers.findAllByOrderByDisplayNameAsc().stream()
                .map(p -> new ProviderAdminView(p, availabilities.findByProviderIdOrderByWeekdayAscStartTimeAsc(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicClosure> upcomingClosures() {
        return closures.findByDateGreaterThanEqualOrderByDateAsc(time.today());
    }

    @Transactional
    public Provider create(String displayName, String serviceLabel) {
        Provider p = new Provider();
        applyNames(p, displayName, serviceLabel);
        return providers.save(p);
    }

    @Transactional
    public Provider update(UUID id, String displayName, String serviceLabel, Boolean active) {
        Provider p = providers.findById(id).orElseThrow(() -> ApiException.notFound("Provider not found."));
        applyNames(p, displayName, serviceLabel);
        if (active != null) p.setActive(active);
        return p;
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        providers.findById(id).orElseThrow(() -> ApiException.notFound("Provider not found.")).setActive(active);
    }

    @Transactional
    public ProviderAvailability addHours(UUID providerId, AvailabilityInput in) {
        requireProvider(providerId);
        validate(in);
        for (ProviderAvailability existing : availabilities.findByProviderIdAndWeekday(providerId, in.weekday())) {
            if (overlaps(existing.getStartTime(), existing.getEndTime(), in.startTime(), in.endTime())) {
                throw ApiException.validation("startTime", "These hours overlap an existing interval on that day.");
            }
        }
        return availabilities.save(toEntity(providerId, in));
    }

    /** Replaces the whole weekly schedule (PUT semantics). */
    @Transactional
    public List<ProviderAvailability> replaceHours(UUID providerId, List<AvailabilityInput> inputs) {
        requireProvider(providerId);
        if (inputs == null) throw ApiException.validation("body", "A list of availability intervals is required.");
        for (int i = 0; i < inputs.size(); i++) {
            validate(inputs.get(i));
            for (int j = 0; j < i; j++) {
                AvailabilityInput a = inputs.get(i), b = inputs.get(j);
                if (a.weekday().equals(b.weekday()) && overlaps(a.startTime(), a.endTime(), b.startTime(), b.endTime())) {
                    throw ApiException.validation("body", "Availability intervals overlap on weekday " + a.weekday() + ".");
                }
            }
        }
        availabilities.deleteByProviderId(providerId);
        availabilities.flush();
        List<ProviderAvailability> saved = new ArrayList<>();
        for (AvailabilityInput in : inputs) saved.add(availabilities.save(toEntity(providerId, in)));
        return saved;
    }

    @Transactional
    public void deleteHours(UUID availabilityId) {
        availabilities.deleteById(availabilityId);
    }

    @Transactional
    public ClinicClosure addClosure(LocalDate date, LocalTime start, LocalTime end, String reason) {
        if (date == null) throw ApiException.validation("date", "Date is required.");
        if ((start == null) != (end == null)) {
            throw ApiException.validation("startTime", "Give both start and end time for a partial closure, or neither.");
        }
        if (start != null && !end.isAfter(start)) {
            throw ApiException.validation("endTime", "End time must be after start time.");
        }
        if (reason != null && reason.length() > 100) {
            throw ApiException.validation("reasonLabel", "Reason must be 100 characters or fewer.");
        }
        ClinicClosure c = new ClinicClosure();
        c.setDate(date);
        c.setStartTime(start);
        c.setEndTime(end);
        c.setReasonLabel(reason == null || reason.isBlank() ? "Closed" : reason.trim());
        return closures.save(c);
    }

    @Transactional
    public void deleteClosure(UUID id) {
        closures.deleteById(id);
    }

    // ---- helpers ----
    private void requireProvider(UUID id) {
        if (!providers.existsById(id)) throw ApiException.notFound("Provider not found.");
    }

    private void applyNames(Provider p, String displayName, String serviceLabel) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 100) {
            throw ApiException.validation("displayName", "Name is required (max 100 characters).");
        }
        if (serviceLabel == null || serviceLabel.isBlank() || serviceLabel.length() > 100) {
            throw ApiException.validation("serviceLabel", "Service label is required (max 100 characters).");
        }
        p.setDisplayName(displayName.trim());
        p.setServiceLabel(serviceLabel.trim());
    }

    private void validate(AvailabilityInput in) {
        if (in == null || in.weekday() == null || in.startTime() == null || in.endTime() == null || in.slotMinutes() == null) {
            throw ApiException.validation("body", "weekday, startTime, endTime and slotMinutes are required.");
        }
        if (in.weekday() < 1 || in.weekday() > 7) throw ApiException.validation("weekday", "Weekday must be 1 (Mon) to 7 (Sun).");
        if (!in.endTime().isAfter(in.startTime())) throw ApiException.validation("endTime", "End time must be after start time.");
        if (in.slotMinutes() < 5 || in.slotMinutes() > 240) throw ApiException.validation("slotMinutes", "Slot length must be 5 to 240 minutes.");
    }

    private boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private ProviderAvailability toEntity(UUID providerId, AvailabilityInput in) {
        ProviderAvailability a = new ProviderAvailability();
        a.setProviderId(providerId);
        a.setWeekday(in.weekday());
        a.setStartTime(in.startTime());
        a.setEndTime(in.endTime());
        a.setSlotMinutes(in.slotMinutes());
        return a;
    }
}
