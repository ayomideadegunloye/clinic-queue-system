package com.example.clinic.web;

import com.example.clinic.appointment.AppointmentService;
import com.example.clinic.appointment.AvailabilityService;
import com.example.clinic.auth.AppUser;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.Provider;
import com.example.clinic.provider.ProviderService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PatientWebController {
    private final ProviderService providers;
    private final AvailabilityService availability;
    private final AppointmentService appointments;
    private final ClinicTime time;

    public PatientWebController(ProviderService providers, AvailabilityService availability,
                                AppointmentService appointments, ClinicTime time) {
        this.providers = providers;
        this.availability = availability;
        this.appointments = appointments;
        this.time = time;
    }

    @GetMapping("/book")
    String book(@RequestParam(required = false) UUID providerId,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                Model model) {
        List<Provider> active = providers.activeProviders();
        LocalDate day = date == null ? time.today() : date;
        model.addAttribute("providers", active);
        model.addAttribute("providerId", providerId);
        model.addAttribute("date", day);
        model.addAttribute("today", time.today());
        if (providerId != null) {
            model.addAttribute("slots", availability.openSlots(providerId, day));
        }
        return "book";
    }

    @PostMapping("/book")
    String doBook(@AuthenticationPrincipal AppUser user, @RequestParam UUID providerId,
                  @RequestParam Instant startAt, RedirectAttributes redirect) {
        var booked = appointments.book(user.getId(), providerId, startAt);
        redirect.addFlashAttribute("success",
                "Booked with " + booked.providerName() + " on " + time.format(booked.startAt()) + ".");
        return "redirect:/appointments";
    }

    @GetMapping("/appointments")
    String mine(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("appointments", appointments.mine(user.getId()));
        return "appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    String cancel(@AuthenticationPrincipal AppUser user, @PathVariable UUID id, RedirectAttributes redirect) {
        appointments.cancel(user, id);
        redirect.addFlashAttribute("success", "Appointment cancelled.");
        return "redirect:/appointments";
    }
}
