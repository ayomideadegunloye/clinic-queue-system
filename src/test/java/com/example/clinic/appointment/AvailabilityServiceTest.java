package com.example.clinic.appointment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.clinic.appointment.AvailabilityService.Slot;
import com.example.clinic.config.ClinicProperties;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.*;
import com.example.clinic.queue.VisitStatus;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {
    // Sat 7 Nov 2026 12:00 UTC. Clinic is in New York (EST, UTC-5 after DST ended 1 Nov).
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
    static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    final UUID providerId = UUID.randomUUID();
    ProviderRepository providers = mock(ProviderRepository.class);
    ProviderAvailabilityRepository availabilities = mock(ProviderAvailabilityRepository.class);
    ClinicClosureRepository closures = mock(ClinicClosureRepository.class);
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    AvailabilityService service;

    @BeforeEach
    void setUp() {
        ClinicProperties props = new ClinicProperties("America/New_York", 2, 30, true, 0, false);
        ClinicTime time = new ClinicTime(props, CLOCK);
        service = new AvailabilityService(providers, availabilities, closures, appointments, time, props, CLOCK);
        when(closures.findByDate(any())).thenReturn(List.of());
        when(appointments.findActive(eq(providerId), any(), any())).thenReturn(List.of());
    }

    private void hours(String from, String to, int slot) {
        ProviderAvailability a = new ProviderAvailability();
        a.setProviderId(providerId);
        a.setWeekday(1);
        a.setStartTime(LocalTime.parse(from));
        a.setEndTime(LocalTime.parse(to));
        a.setSlotMinutes(slot);
        when(availabilities.findByProviderIdAndWeekday(providerId, 1)).thenReturn(List.of(a));
    }

    @Test
    void generatesSlotsInsideWorkingInterval() {
        hours("09:00", "10:00", 20);
        List<Slot> slots = service.computeSlots(providerId, MONDAY);
        assertEquals(3, slots.size());
        assertEquals(Instant.parse("2026-11-09T14:00:00Z"), slots.get(0).startAt()); // 09:00 EST
    }

    @Test
    void slotMustFitFullyInsideInterval() {
        hours("09:00", "09:50", 20);
        assertEquals(2, service.computeSlots(providerId, MONDAY).size());
    }

    @Test
    void bookedSlotIsRemoved() {
        hours("09:00", "10:00", 20);
        Appointment booked = new Appointment();
        booked.setProviderId(providerId);
        booked.setStartAt(Instant.parse("2026-11-09T14:20:00Z"));
        booked.setEndAt(Instant.parse("2026-11-09T14:40:00Z"));
        booked.setStatus(VisitStatus.BOOKED);
        when(appointments.findActive(eq(providerId), any(), any())).thenReturn(List.of(booked));

        List<Instant> starts = service.computeSlots(providerId, MONDAY).stream().map(Slot::startAt).toList();
        assertEquals(List.of(Instant.parse("2026-11-09T14:00:00Z"), Instant.parse("2026-11-09T14:40:00Z")), starts);
    }

    @Test
    void fullDayClosureRemovesEverything() {
        hours("09:00", "10:00", 20);
        ClinicClosure c = new ClinicClosure();
        c.setDate(MONDAY);
        c.setReasonLabel("Holiday");
        when(closures.findByDate(MONDAY)).thenReturn(List.of(c));
        assertTrue(service.computeSlots(providerId, MONDAY).isEmpty());
    }

    @Test
    void partialClosureRemovesOverlappingSlots() {
        hours("09:00", "10:00", 20);
        ClinicClosure c = new ClinicClosure();
        c.setDate(MONDAY);
        c.setStartTime(LocalTime.of(9, 30));
        c.setEndTime(LocalTime.of(10, 0));
        c.setReasonLabel("Staff meeting");
        when(closures.findByDate(MONDAY)).thenReturn(List.of(c));
        // 09:20-09:40 and 09:40-10:00 both overlap 09:30-10:00
        assertEquals(1, service.computeSlots(providerId, MONDAY).size());
    }

    @Test
    void pastAndBeyondHorizonDatesHaveNoSlots() {
        hours("09:00", "10:00", 20);
        assertTrue(service.computeSlots(providerId, LocalDate.of(2026, 11, 2)).isEmpty());
        assertTrue(service.computeSlots(providerId, LocalDate.of(2027, 3, 1)).isEmpty());
    }
}
