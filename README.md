# Community Clinic Appointment & Queue System

A scheduling and front-desk tool for a small community clinic. Patients book open slots and see their place in the day; reception checks people in, adds walk-ins and moves visits through the queue; admins manage providers, hours, closures and staff.

> **Scope boundary:** this is an administrative scheduling tool, not an electronic medical record. It stores names, optional minimal contact, and appointment/queue state. It does not store diagnoses, prescriptions, notes or insurance details, and it should not be extended to.

## Stack

Java 25 (latest LTS), Maven, Spring Boot 3.3 (Web, Data JPA, Validation, Security), Thymeleaf (server-rendered UI, hand-written responsive CSS, no CDN), PostgreSQL 16, Flyway, JUnit 5 + Mockito.

## Run it

Requirements: JDK 25, Maven 3.9+, Docker (for PostgreSQL).

```bash
docker compose up -d        # PostgreSQL on :5432 (db/user/password: clinic)
mvn spring-boot:run         # app on http://localhost:8080
mvn test                    # unit tests
```

On first start Flyway creates the schema and the app seeds fictional demo data (`CLINIC_SEED_DEMO=false` turns that off). Copy `.env.example` only if you want to override defaults.

| Role | Email | Password |
|---|---|---|
| Admin | admin@clinic.test | DemoPass123! |
| Receptionist | reception@clinic.test | DemoPass123! |
| Patient | patient@clinic.test | DemoPass123! |
| Patient | patient2@clinic.test | DemoPass123! |

Demo flow: sign in as the patient and book a slot, then sign in as the receptionist, open **Queue**, and check in, call, start and complete the visit. Add a walk-in from the same screen.

## Documented defaults (project decisions)

| Setting | Default | Property |
|---|---|---|
| Clinic timezone | `America/New_York` | `clinic.timezone` / `CLINIC_TIMEZONE` |
| Patient cancellation cutoff | 2 hours before start | `clinic.cancellation-cutoff-hours` |
| Booking horizon | 30 days | `clinic.booking-horizon-days` |
| Walk-in priority | 0 (arrival time competes with scheduled time) | `clinic.walk-in-priority-minutes` |
| Show queue position to patients | on | `clinic.show-queue-position` |

**Queue ordering:** scheduled visits sort by appointment start time; walk-ins sort by check-in time (minus the priority setting). Queue position is a live estimate among `WAITING` visits for the same provider, not a promised wait time.

## Architecture

```
web/ api/        controllers (MVC pages, REST JSON), no business rules
appointment/     availability calculation, booking, cancellation
queue/           visit status machine, walk-ins, daily queue, audit events
provider/        providers, weekly hours, closures
auth/            accounts, roles, registration, principal
config/          security, timezone helper, properties, demo seeder
common/          ApiException / ApiError
```

Status model lives in one place (`VisitStatus.actions()`); the server applies only those transitions and records who changed what in `status_event`. Every booking also creates a `queue_entry` (status `BOOKED`), so staff manage one list for scheduled visits and walk-ins. Check-in is `BOOKED -> WAITING`.

### Double-booking protection (three layers)
1. `SELECT ... FOR UPDATE` on the provider row serialises concurrent bookings for that provider.
2. Availability is recomputed inside the booking transaction.
3. A PostgreSQL exclusion constraint (`appointment_no_overlap`, `btree_gist`) rejects any overlapping non-cancelled appointment for a provider, whatever the application does.

Cancelled bookings are excluded from the constraint, so their slot becomes available again.

## Security and privacy

BCrypt password hashing; role checks on every `/staff`, `/admin` and `/api/staff|admin` route; patients can only read or cancel their own appointments (others get a 404, not a 403); CSRF enabled for browser sessions; the REST API is stateless HTTP Basic; secrets come from environment variables; no stack traces in responses; registration errors do not reveal whether an email exists. Set `SESSION_COOKIE_SECURE=true` behind HTTPS.

## REST API examples

```bash
# register a patient
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"new@clinic.test","password":"CorrectHorse1","displayName":"New Patient"}'

# providers, then open slots (ISO date)
curl localhost:8080/api/providers
curl -u patient@clinic.test:DemoPass123! "localhost:8080/api/providers/<providerId>/availability?date=2026-11-09"

# book, list, cancel
curl -u patient@clinic.test:DemoPass123! -X POST localhost:8080/api/appointments \
  -H 'Content-Type: application/json' -d '{"providerId":"<id>","startAt":"2026-11-09T14:00:00Z"}'
curl -u patient@clinic.test:DemoPass123! localhost:8080/api/appointments/mine
curl -u patient@clinic.test:DemoPass123! -X DELETE localhost:8080/api/appointments/<appointmentId>

# staff: queue, walk-in, status change
curl -u reception@clinic.test:DemoPass123! "localhost:8080/api/staff/queue?date=2026-11-09"
curl -u reception@clinic.test:DemoPass123! -X POST localhost:8080/api/staff/walk-ins \
  -H 'Content-Type: application/json' -d '{"displayName":"Walk In","providerId":"<id>"}'
curl -u reception@clinic.test:DemoPass123! -X PATCH localhost:8080/api/staff/queue/<entryId>/status \
  -H 'Content-Type: application/json' -d '{"status":"CALLED"}'

# admin: weekly hours (replaces the whole schedule), closures, staff
curl -u admin@clinic.test:DemoPass123! -X PUT localhost:8080/api/admin/providers/<id>/availability \
  -H 'Content-Type: application/json' \
  -d '[{"weekday":1,"startTime":"09:00","endTime":"12:00","slotMinutes":20}]'
```

Errors share one shape: `{"code":"SLOT_UNAVAILABLE","message":"...","fieldErrors":{}}` with `VALIDATION_ERROR` (400), `FORBIDDEN` (403), `NOT_FOUND` (404), `SLOT_UNAVAILABLE` (409), `INVALID_STATUS_TRANSITION` (409), `CANCELLATION_TOO_LATE` (409).

## Known limitations

- Changing a provider's hours does not move or cancel appointments already booked outside the new hours.
- Closures apply clinic-wide, not per provider.
- No email/SMS; the UI shows status in-app and does not auto-refresh (reload to update).
- Receptionists cancel via queue actions; only patients (own bookings) and admins use the cancel endpoint.
- No rate limiting or account lockout; no password reset.
- Demo credentials are public. Turn seeding off for anything beyond local use.
- Not production-hardened (no TLS config, no deployment manifests).

## Roadmap

Real-time queue refresh, email reminders, per-provider closures, waitlist, CSV daily export, Testcontainers integration tests for concurrent booking.

## Layout

```
src/main/java            application code
src/main/resources       templates, CSS, application.yml, db/migration (Flyway)
src/test/java            unit tests
docker-compose.yml  .env.example  .gitignore  README.md
```
Add `screenshots/` and a short demo recording once you run it.
