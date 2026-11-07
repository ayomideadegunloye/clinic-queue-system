package com.example.clinic.api;

import com.example.clinic.appointment.AppointmentService;
import com.example.clinic.appointment.AppointmentService.AppointmentView;
import com.example.clinic.auth.AppUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {
    private final AppointmentService appointments;

    public AppointmentApiController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentView book(@AuthenticationPrincipal AppUser user, @Valid @RequestBody Requests.Book body) {
        return appointments.book(user.getId(), body.providerId(), body.startAt());
    }

    @GetMapping("/mine")
    public List<AppointmentView> mine(@AuthenticationPrincipal AppUser user) {
        return appointments.mine(user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        appointments.cancel(user, id);
    }
}
