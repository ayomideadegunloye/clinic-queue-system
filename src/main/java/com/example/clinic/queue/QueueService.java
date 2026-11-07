package com.example.clinic.queue;

import com.example.clinic.appointment.Appointment;
import com.example.clinic.appointment.AppointmentRepository;
import com.example.clinic.auth.PatientProfile;
import com.example.clinic.auth.PatientProfileRepository;
import com.example.clinic.common.ApiException;
import com.example.clinic.config.ClinicProperties;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.Provider;
import com.example.clinic.provider.ProviderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the visit lifecycle. Queue ordering rule: scheduled visits sort by appointment start,
 * walk-ins by arrival time (minus clinic.walk-in-priority-minutes). Position is an estimate, not a wait time.
 */
@Service
public class QueueService {
    public record QueueItem(UUID id, UUID appointmentId, String patientName, String providerName, String serviceLabel,
                            Instant scheduledAt, Instant checkedInAt, VisitStatus status, Integer position,
                            boolean walkIn, List<VisitStatus.Action> actions) {}

    private final QueueEntryRepository entries;
    private final StatusEventRepository events;
    private final AppointmentRepository appointments;
    private final PatientProfileRepository patients;
    private final ProviderRepository providers;
    private final ClinicTime time;
    private final ClinicProperties props;
    private final Clock clock;

    public QueueService(QueueEntryRepository entries, StatusEventRepository events, AppointmentRepository appointments,
                        PatientProfileRepository patients, ProviderRepository providers,
                        ClinicTime time, ClinicProperties props, Clock clock) {
        this.entries = entries;
        this.events = events;
        this.appointments = appointments;
        this.patients = patients;
        this.providers = providers;
        this.time = time;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public QueueEntry createForAppointment(Appointment a, UUID actorUserId) {
        QueueEntry e = new QueueEntry();
        e.setAppointmentId(a.getId());
        e.setPatientId(a.getPatientId());
        e.setProviderId(a.getProviderId());
        e.setQueueDate(time.dateOf(a.getStartAt()));
        e.setStatus(VisitStatus.BOOKED);
        e = entries.save(e);
        events.save(StatusEvent.of(e.getId(), null, VisitStatus.BOOKED, actorUserId, clock.instant()));
        return e;
    }

    @Transactional
    public QueueItem registerWalkIn(String displayName, String contact, UUID providerId, UUID actorUserId) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 100) {
            throw ApiException.validation("displayName", "Name is required (max 100 characters).");
        }
        if (contact != null && contact.length() > 100) {
            throw ApiException.validation("contact", "Contact must be 100 characters or fewer.");
        }
        Provider provider = providers.findById(providerId).filter(Provider::isActive)
                .orElseThrow(() -> ApiException.notFound("Provider not found."));
        PatientProfile profile = new PatientProfile();
        profile.setDisplayName(displayName.trim());
        profile.setContact(contact == null || contact.isBlank() ? null : contact.trim());
        profile = patients.save(profile);

        Instant now = clock.instant();
        QueueEntry e = new QueueEntry();
        e.setPatientId(profile.getId());
        e.setProviderId(provider.getId());
        e.setQueueDate(time.today());
        e.setCheckedInAt(now);
        e.setStatus(VisitStatus.WAITING);
        e = entries.save(e);
        events.save(StatusEvent.of(e.getId(), null, VisitStatus.WAITING, actorUserId, now));
        return itemFor(e);
    }

    @Transactional
    public QueueItem transition(UUID entryId, VisitStatus target, UUID actorUserId) {
        if (target == null) throw ApiException.validation("status", "Target status is required.");
        QueueEntry e = entries.findForUpdate(entryId).orElseThrow(() -> ApiException.notFound("Queue entry not found."));
        VisitStatus from = e.getStatus();
        if (!from.canMoveTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "A visit cannot move from " + from + " to " + target + ".");
        }
        boolean checkIn = from == VisitStatus.BOOKED && target == VisitStatus.WAITING;
        if (checkIn && !e.getQueueDate().equals(time.today())) {
            throw ApiException.validation("status", "Only today's appointments can be checked in.");
        }
        Instant now = clock.instant();
        e.setStatus(target);
        if (checkIn) e.setCheckedInAt(now);
        if (e.getAppointmentId() != null) {
            appointments.findById(e.getAppointmentId()).ifPresent(a -> a.setStatus(target));
        }
        events.save(StatusEvent.of(e.getId(), from, target, actorUserId, now));
        return itemFor(e);
    }

    @Transactional(readOnly = true)
    public List<QueueItem> dailyQueue(LocalDate date) {
        return buildItems(entries.findByQueueDate(date));
    }

    private QueueItem itemFor(QueueEntry e) {
        return dailyQueue(e.getQueueDate()).stream()
                .filter(i -> i.id().equals(e.getId()))
                .findFirst()
                .orElseThrow();
    }

    private List<QueueItem> buildItems(List<QueueEntry> list) {
        Map<UUID, PatientProfile> pats = patients.findAllById(list.stream().map(QueueEntry::getPatientId).toList())
                .stream().collect(Collectors.toMap(PatientProfile::getId, Function.identity()));
        Map<UUID, Provider> provs = providers.findAllById(list.stream().map(QueueEntry::getProviderId).toList())
                .stream().collect(Collectors.toMap(Provider::getId, Function.identity()));
        Map<UUID, Appointment> appts = appointments.findAllById(
                        list.stream().map(QueueEntry::getAppointmentId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Appointment::getId, Function.identity()));

        Map<UUID, Instant> sortKey = new HashMap<>();
        for (QueueEntry e : list) {
            Appointment a = e.getAppointmentId() == null ? null : appts.get(e.getAppointmentId());
            Instant key = a != null ? a.getStartAt()
                    : e.getCheckedInAt().minus(Duration.ofMinutes(props.walkInPriorityMinutes()));
            sortKey.put(e.getId(), key);
        }
        Comparator<QueueEntry> byKey = Comparator.<QueueEntry, Instant>comparing(e -> sortKey.get(e.getId()))
                .thenComparing(QueueEntry::getCreatedAt);

        // position among WAITING entries of the same provider
        Map<UUID, Integer> positions = new HashMap<>();
        list.stream().filter(e -> e.getStatus() == VisitStatus.WAITING)
                .collect(Collectors.groupingBy(QueueEntry::getProviderId))
                .values().forEach(group -> {
                    group.sort(byKey);
                    for (int i = 0; i < group.size(); i++) positions.put(group.get(i).getId(), i + 1);
                });

        return list.stream()
                .sorted(Comparator.comparingInt((QueueEntry e) -> rank(e.getStatus())).thenComparing(byKey))
                .map(e -> {
                    Appointment a = e.getAppointmentId() == null ? null : appts.get(e.getAppointmentId());
                    PatientProfile p = pats.get(e.getPatientId());
                    Provider pr = provs.get(e.getProviderId());
                    return new QueueItem(e.getId(), e.getAppointmentId(),
                            p == null ? "Unknown" : p.getDisplayName(),
                            pr == null ? "Unknown" : pr.getDisplayName(),
                            pr == null ? "" : pr.getServiceLabel(),
                            a == null ? null : a.getStartAt(), e.getCheckedInAt(), e.getStatus(),
                            positions.get(e.getId()), a == null, e.getStatus().actions());
                })
                .toList();
    }

    private static int rank(VisitStatus s) {
        return switch (s) {
            case IN_SERVICE -> 0;
            case CALLED -> 1;
            case WAITING -> 2;
            case BOOKED -> 3;
            default -> 4;
        };
    }
}
