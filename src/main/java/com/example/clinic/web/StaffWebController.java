package com.example.clinic.web;

import com.example.clinic.auth.AppUser;
import com.example.clinic.config.ClinicTime;
import com.example.clinic.provider.ProviderService;
import com.example.clinic.queue.QueueService;
import com.example.clinic.queue.VisitStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffWebController {
    private final QueueService queue;
    private final ProviderService providers;
    private final ClinicTime time;

    public StaffWebController(QueueService queue, ProviderService providers, ClinicTime time) {
        this.queue = queue;
        this.providers = providers;
        this.time = time;
    }

    @GetMapping("/queue")
    String queue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                 Model model) {
        LocalDate day = date == null ? time.today() : date;
        model.addAttribute("date", day);
        model.addAttribute("isToday", day.equals(time.today()));
        model.addAttribute("prev", day.minusDays(1));
        model.addAttribute("next", day.plusDays(1));
        model.addAttribute("items", queue.dailyQueue(day));
        model.addAttribute("providers", providers.activeProviders());
        return "staff/queue";
    }

    @PostMapping("/walk-in")
    String walkIn(@AuthenticationPrincipal AppUser user, @RequestParam String displayName,
                  @RequestParam(required = false) String contact, @RequestParam UUID providerId,
                  RedirectAttributes redirect) {
        queue.registerWalkIn(displayName, contact, providerId, user.getId());
        redirect.addFlashAttribute("success", "Walk-in added to today's queue.");
        return "redirect:/staff/queue";
    }

    @PostMapping("/queue/{id}/status")
    String status(@AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam VisitStatus status,
                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        queue.transition(id, status, user.getId());
        return "redirect:/staff/queue" + (date == null ? "" : "?date=" + date);
    }
}
