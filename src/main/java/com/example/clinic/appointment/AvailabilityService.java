package com.example.clinic.appointment;

import com.example.clinic.common.ApiException;
import com.example.clinic.config.ClinicProperties;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes bookable slots from weekly hours, closures, existing bookings, and the current time. */
@Service
public class AvailabilityService {
    public record Slot(Instant startAt, Instant endAt) {}

    private final ProviderRepository providers;
    private final ProviderAvailabilityRepository availabilities;
    private final ClinicClosureRepository closures;
    private final AppointmentRepository appointments;
    private final ClinicTime time;
    private final ClinicProperties props;
    private final Clock clock;

    public AvailabilityService(ProviderRepository providers, ProviderAvailabilityRepository availabilities,
                               ClinicClosureRepository closures, AppointmentRepository appointments,
                               ClinicTime time, ClinicProperties props, Clock clock) {
        this.providers = providers;
        this.availabilities = availabilities;
        this.closures = closures;
        this.appointments = appointments;
        this.time = time;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Slot> openSlots(UUID providerId, LocalDate date) {
        Provider provider = providers.findById(providerId)
                .filter(Provider::isActive)
                .orElseThrow(() -> ApiException.notFound("Provider not found."));
        return computeSlots(provider.getId(), date);
    }

    @Transactional(readOnly = true)
    public Optional<Slot> findSlot(UUID providerId, Instant startAt) {
        return openSlots(providerId, time.dateOf(startAt)).stream()
                .filter(s -> s.startAt().equals(startAt))
                .findFirst();
    }

    List<Slot> computeSlots(UUID providerId, LocalDate date) {
        LocalDate today = time.today();
        if (date == null || date.isBefore(today) || date.isAfter(today.plusDays(props.bookingHorizonDays()))) {
            return List.of();
        }
        List<ClinicClosure> dayClosures = closures.findByDate(date);
        if (dayClosures.stream().anyMatch(ClinicClosure::isFullDay)) {
            return List.of();
        }
        List<Appointment> booked = appointments.findActive(providerId,
                time.startOfDay(date), time.startOfDay(date.plusDays(1)));
        Instant now = clock.instant();

        List<Slot> slots = new ArrayList<>();
        for (ProviderAvailability window : availabilities.findByProviderIdAndWeekday(providerId, date.getDayOfWeek().getValue())) {
            LocalTime t = window.getStartTime();
            while (true) {
                LocalTime slotEnd = t.plusMinutes(window.getSlotMinutes());
                // stop if the slot would wrap past midnight or spill outside the working interval
                if (!slotEnd.isAfter(t) || slotEnd.isAfter(window.getEndTime())) break;
                final LocalTime start = t;
                boolean inClosure = dayClosures.stream()
                        .anyMatch(c -> start.isBefore(c.getEndTime()) && slotEnd.isAfter(c.getStartTime()));
                Instant startAt = time.toInstant(date, t);
                Instant endAt = time.toInstant(date, slotEnd);
                boolean taken = booked.stream()
                        .anyMatch(a -> a.getStartAt().isBefore(endAt) && a.getEndAt().isAfter(startAt));
                if (!inClosure && !taken && startAt.isAfter(now)) {
                    slots.add(new Slot(startAt, endAt));
                }
                t = slotEnd;
            }
        }
        slots.sort(Comparator.comparing(Slot::startAt));
        return slots.stream().distinct().toList();
    }
}
