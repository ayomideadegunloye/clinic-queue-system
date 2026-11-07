package com.example.clinic.config;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** All clinic-timezone conversions live here. Instants are stored in UTC; people see clinic-local time. */
@Component("clinicTime")
public class ClinicTime {
    private final ZoneId zone;
    private final Clock clock;
    private final DateTimeFormatter dateTime;
    private final DateTimeFormatter timeOnly;
    private final DateTimeFormatter dateOnly;

    public ClinicTime(ClinicProperties props, Clock clock) {
        this.zone = props.zone();
        this.clock = clock;
        this.dateTime = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH).withZone(zone);
        this.timeOnly = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withZone(zone);
        this.dateOnly = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    }

    public ZoneId zone() { return zone; }

    public LocalDate today() { return LocalDate.now(clock.withZone(zone)); }

    public LocalDate dateOf(Instant instant) { return instant.atZone(zone).toLocalDate(); }

    public Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, zone).toInstant();
    }

    public Instant startOfDay(LocalDate date) { return date.atStartOfDay(zone).toInstant(); }

    public String format(Instant instant) { return instant == null ? "" : dateTime.format(instant); }

    public String time(Instant instant) { return instant == null ? "" : timeOnly.format(instant); }

    public String date(LocalDate date) { return date == null ? "" : dateOnly.format(date); }

    public String dayName(int isoWeekday) {
        return DayOfWeek.of(isoWeekday).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
