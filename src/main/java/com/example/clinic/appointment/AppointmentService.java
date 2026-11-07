package com.example.clinic.appointment;

import com.example.clinic.appointment.AvailabilityService.Slot;
import com.example.clinic.auth.*;
import com.example.clinic.common.ApiException;
import com.example.clinic.config.ClinicProperties;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.Provider;
import com.example.clinic.provider.ProviderRepository;
import com.example.clinic.queue.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    public record AppointmentView(UUID id, String providerName, String serviceLabel, Instant startAt, Instant endAt,
                                  VisitStatus status, Integer queuePosition, boolean cancellable) {}

    private final AppointmentRepository appointments;
    private final ProviderRepository providers;
    private final PatientProfileRepository patients;
    private final QueueEntryRepository entries;
    private final AvailabilityService availability;
    private final QueueService queue;
    private final AuthService auth;
    private final ClinicProperties props;
    private final ClinicTime time;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, ProviderRepository providers,
                              PatientProfileRepository patients, QueueEntryRepository entries,
                              AvailabilityService availability, QueueService queue, AuthService auth,
                              ClinicProperties props, ClinicTime time, Clock clock) {
        this.appointments = appointments;
        this.providers = providers;
        this.patients = patients;
        this.entries = entries;
        this.availability = availability;
        this.queue = queue;
        this.auth = auth;
        this.props = props;
        this.time = time;
        this.clock = clock;
    }

    /**
     * Books a slot. Defence in depth against double-booking:
     * 1) provider row lock serialises concurrent requests, 2) availability is recomputed inside the transaction,
     * 3) the database exclusion constraint rejects any overlap that still slips through.
     */
    @Transactional
    public AppointmentView book(UUID userId, UUID providerId, Instant startAt) {
        if (providerId == null || startAt == null) {
            throw ApiException.validation("startAt", "Provider and start time are required.");
        }
        PatientProfile patient = auth.profileFor(userId);
        Provider provider = providers.findForUpdate(providerId).filter(Provider::isActive)
                .orElseThrow(() -> ApiException.notFound("Provider not found."));
        Slot slot = availability.findSlot(provider.getId(), startAt).orElseThrow(ApiException::slotUnavailable);

        Appointment a = new Appointment();
        a.setPatientId(patient.getId());
        a.setProviderId(provider.getId());
        a.setStartAt(slot.startAt());
        a.setEndAt(slot.endAt());
        a.setStatus(VisitStatus.BOOKED);
        try {
            a = appointments.saveAndFlush(a);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.slotUnavailable();
        }
        queue.createForAppointment(a, userId);
        return view(a, provider, null);
    }

    @Transactional(readOnly = true)
    public List<AppointmentView> mine(UUID userId) {
        Optional<PatientProfile> profile = patients.findByUserId(userId);
        if (profile.isEmpty()) return List.of();
        List<Appointment> list = appointments.findByPatientIdOrderByStartAtDesc(profile.get().getId());
        Map<UUID, Provider> provs = providers.findAllById(list.stream().map(Appointment::getProviderId).toList())
                .stream().collect(Collectors.toMap(Provider::getId, Function.identity()));
        Map<LocalDate, List<QueueService.QueueItem>> dayCache = new HashMap<>();
        List<AppointmentView> out = new ArrayList<>();
        for (Appointment a : list) {
            Integer position = null;
            if (props.showQueuePosition() && a.getStatus() == VisitStatus.WAITING) {
                LocalDate day = time.dateOf(a.getStartAt());
                List<QueueService.QueueItem> items = dayCache.computeIfAbsent(day, queue::dailyQueue);
                for (QueueService.QueueItem item : items) {
                    if (a.getId().equals(item.appointmentId())) position = item.position();
                }
            }
            out.add(view(a, provs.get(a.getProviderId()), position));
        }
        return out;
    }

    @Transactional
    public void cancel(AppUser actor, UUID appointmentId) {
        Appointment a = appointments.findById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        if (actor.getRole() != Role.ADMIN) {
            if (actor.getRole() != Role.PATIENT) {
                throw ApiException.forbidden("Front-desk staff change visits from the queue screen.");
            }
            PatientProfile owner = patients.findById(a.getPatientId())
                    .orElseThrow(() -> ApiException.notFound("Appointment not found."));
            // Same response as "does not exist" so other patients' bookings are not revealed.
            if (!actor.getId().equals(owner.getUserId())) {
                throw ApiException.notFound("Appointment not found.");
            }
            if (!beforeCutoff(a)) {
                throw ApiException.conflict("CANCELLATION_TOO_LATE",
                        "Appointments can only be cancelled at least " + props.cancellationCutoffHours()
                                + " hours before the start time. Please contact the clinic.");
            }
        }
        QueueEntry entry = entries.findByAppointmentId(a.getId())
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        if (entry.getStatus() != VisitStatus.BOOKED) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Only upcoming bookings can be cancelled.");
        }
        queue.transition(entry.getId(), VisitStatus.CANCELLED, actor.getId());
    }

    private boolean beforeCutoff(Appointment a) {
        return clock.instant().isBefore(a.getStartAt().minus(Duration.ofHours(props.cancellationCutoffHours())));
    }

    private AppointmentView view(Appointment a, Provider p, Integer position) {
        boolean cancellable = a.getStatus() == VisitStatus.BOOKED && beforeCutoff(a);
        return new AppointmentView(a.getId(), p == null ? "Unknown" : p.getDisplayName(),
                p == null ? "" : p.getServiceLabel(), a.getStartAt(), a.getEndAt(), a.getStatus(), position, cancellable);
    }
}
