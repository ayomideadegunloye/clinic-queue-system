package com.example.clinic.config;

import com.example.clinic.appointment.AppointmentService;
import com.example.clinic.appointment.AvailabilityService;
import com.example.clinic.auth.AuthService;
import com.example.clinic.auth.Role;
import com.example.clinic.auth.UserAccount;
import com.example.clinic.auth.UserAccountRepository;
import com.example.clinic.provider.Provider;
import com.example.clinic.provider.ProviderService;
import com.example.clinic.provider.ProviderService.AvailabilityInput;
import com.example.clinic.queue.QueueService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds clearly fictional demo data on first start (clinic.seed-demo=true and an empty user table).
 * Demo password is intentionally public; disable seeding outside local demos.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "DemoPass123!";

    private final ClinicProperties props;
    private final UserAccountRepository users;
    private final AuthService auth;
    private final ProviderService providers;
    private final AvailabilityService availability;
    private final AppointmentService appointments;
    private final QueueService queue;
    private final ClinicTime time;

    public DataSeeder(ClinicProperties props, UserAccountRepository users, AuthService auth, ProviderService providers,
                      AvailabilityService availability, AppointmentService appointments, QueueService queue,
                      ClinicTime time) {
        this.props = props;
        this.users = users;
        this.auth = auth;
        this.providers = providers;
        this.availability = availability;
        this.appointments = appointments;
        this.queue = queue;
        this.time = time;
    }

    @Override
    public void run(String... args) {
        if (!props.seedDemo() || users.count() > 0) return;

        UserAccount admin = auth.createStaff("admin@clinic.test", DEMO_PASSWORD, "Demo Admin", Role.ADMIN);
        auth.createStaff("reception@clinic.test", DEMO_PASSWORD, "Demo Receptionist", Role.RECEPTIONIST);
        UserAccount patient = auth.registerPatient("patient@clinic.test", DEMO_PASSWORD, "Sam Demo");
        auth.registerPatient("patient2@clinic.test", DEMO_PASSWORD, "Alex Example");

        Provider gp = providers.create("Dr. Amara Okafor", "General practice");
        Provider nurse = providers.create("Priya Raman, RN", "Vaccinations & nursing");
        Provider peds = providers.create("Dr. Luis Moreno", "Pediatrics");

        for (int day = 1; day <= 5; day++) {
            providers.addHours(gp.getId(), new AvailabilityInput(day, LocalTime.of(9, 0), LocalTime.of(12, 0), 20));
            providers.addHours(gp.getId(), new AvailabilityInput(day, LocalTime.of(13, 0), LocalTime.of(17, 0), 20));
            providers.addHours(nurse.getId(), new AvailabilityInput(day, LocalTime.of(8, 30), LocalTime.of(12, 30), 15));
        }
        for (int day : new int[]{2, 4}) {
            providers.addHours(peds.getId(), new AvailabilityInput(day, LocalTime.of(10, 0), LocalTime.of(16, 0), 30));
        }

        LocalDate christmas = LocalDate.of(time.today().getYear(), 12, 25);
        if (christmas.isBefore(time.today())) christmas = christmas.plusYears(1);
        providers.addClosure(christmas, null, null, "Holiday closure");

        // One upcoming booking for the demo patient, plus a walk-in on today's queue.
        for (int i = 0; i < 14; i++) {
            var slots = availability.openSlots(gp.getId(), time.today().plusDays(i));
            if (slots.size() > 3) {
                appointments.book(patient.getId(), gp.getId(), slots.get(2).startAt());
                break;
            }
        }
        queue.registerWalkIn("Jordan Walk-in", null, nurse.getId(), admin.getId());
        log.info("Seeded demo data. Demo password for all demo accounts: {}", DEMO_PASSWORD);
    }
}
