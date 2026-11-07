package com.example.clinic.web;

import com.example.clinic.auth.AuthService;
import com.example.clinic.auth.Role;
import com.example.clinic.provider.ProviderService;
import com.example.clinic.provider.ProviderService.AvailabilityInput;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminWebController {
    private final ProviderService providers;
    private final AuthService auth;

    public AdminWebController(ProviderService providers, AuthService auth) {
        this.providers = providers;
        this.auth = auth;
    }

    @GetMapping("/providers")
    String page(Model model) {
        model.addAttribute("views", providers.adminView());
        model.addAttribute("closures", providers.upcomingClosures());
        return "admin/providers";
    }

    @PostMapping("/providers")
    String create(@RequestParam String displayName, @RequestParam String serviceLabel, RedirectAttributes r) {
        providers.create(displayName, serviceLabel);
        r.addFlashAttribute("success", "Provider added. Now add their weekly hours.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/providers/{id}/active")
    String setActive(@PathVariable UUID id, @RequestParam boolean active, RedirectAttributes r) {
        providers.setActive(id, active);
        r.addFlashAttribute("success", active ? "Provider is bookable again." : "Provider hidden from booking.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/providers/{id}/hours")
    String addHours(@PathVariable UUID id, @RequestParam int weekday,
                    @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                    @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
                    @RequestParam int slotMinutes, RedirectAttributes r) {
        providers.addHours(id, new AvailabilityInput(weekday, startTime, endTime, slotMinutes));
        r.addFlashAttribute("success", "Hours added.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/hours/{id}/delete")
    String deleteHours(@PathVariable UUID id, RedirectAttributes r) {
        providers.deleteHours(id);
        r.addFlashAttribute("success", "Hours removed.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/closures")
    String addClosure(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                      @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
                      @RequestParam(required = false) String reasonLabel, RedirectAttributes r) {
        providers.addClosure(date, startTime, endTime, reasonLabel);
        r.addFlashAttribute("success", "Closure added.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/closures/{id}/delete")
    String deleteClosure(@PathVariable UUID id, RedirectAttributes r) {
        providers.deleteClosure(id);
        r.addFlashAttribute("success", "Closure removed.");
        return "redirect:/admin/providers";
    }

    @PostMapping("/staff")
    String createStaff(@RequestParam String displayName, @RequestParam String email, @RequestParam String password,
                       @RequestParam Role role, RedirectAttributes r) {
        auth.createStaff(email, password, displayName, role);
        r.addFlashAttribute("success", "Staff account created.");
        return "redirect:/admin/providers";
    }
}
