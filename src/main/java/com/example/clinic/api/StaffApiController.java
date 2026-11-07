package com.example.clinic.api;

import com.example.clinic.auth.AppUser;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.queue.QueueService;
import com.example.clinic.queue.QueueService.QueueItem;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
public class StaffApiController {
    private final QueueService queue;
    private final ClinicTime time;

    public StaffApiController(QueueService queue, ClinicTime time) {
        this.queue = queue;
        this.time = time;
    }

    @GetMapping("/queue")
    public List<QueueItem> queue(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queue.dailyQueue(date == null ? time.today() : date);
    }

    @PostMapping("/walk-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueItem walkIn(@AuthenticationPrincipal AppUser user, @Valid @RequestBody Requests.WalkIn body) {
        return queue.registerWalkIn(body.displayName(), body.contact(), body.providerId(), user.getId());
    }

    @PatchMapping("/queue/{id}/status")
    public QueueItem status(@AuthenticationPrincipal AppUser user, @PathVariable UUID id,
                            @Valid @RequestBody Requests.Status body) {
        return queue.transition(id, body.status(), user.getId());
    }
}
