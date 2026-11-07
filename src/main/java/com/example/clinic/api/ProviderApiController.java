package com.example.clinic.api;

import com.example.clinic.appointment.AvailabilityService;
import com.example.clinic.appointment.AvailabilityService.Slot;
import com.example.clinic.provider.ProviderService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers")
public class ProviderApiController {
    public record ProviderDto(UUID id, String displayName, String serviceLabel) {}

    private final ProviderService providers;
    private final AvailabilityService availability;

    public ProviderApiController(ProviderService providers, AvailabilityService availability) {
        this.providers = providers;
        this.availability = availability;
    }

    @GetMapping
    public List<ProviderDto> list() {
        return providers.activeProviders().stream()
                .map(p -> new ProviderDto(p.getId(), p.getDisplayName(), p.getServiceLabel()))
                .toList();
    }

    @GetMapping("/{id}/availability")
    public List<Slot> availability(@PathVariable UUID id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availability.openSlots(id, date);
    }
}
