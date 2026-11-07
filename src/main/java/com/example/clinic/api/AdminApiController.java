package com.example.clinic.api;

import com.example.clinic.api.ProviderApiController.ProviderDto;
import com.example.clinic.auth.AuthService;
import com.example.clinic.auth.UserAccount;
import com.example.clinic.provider.ClinicClosure;
import com.example.clinic.provider.Provider;
import com.example.clinic.provider.ProviderAvailability;
import com.example.clinic.provider.ProviderService;
import com.example.clinic.provider.ProviderService.AvailabilityInput;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final ProviderService providers;
    private final AuthService auth;

    public AdminApiController(ProviderService providers, AuthService auth) {
        this.providers = providers;
        this.auth = auth;
    }

    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderDto create(@Valid @RequestBody Requests.ProviderUpsert body) {
        return dto(providers.create(body.displayName(), body.serviceLabel()));
    }

    @PutMapping("/providers/{id}")
    public ProviderDto update(@PathVariable UUID id, @Valid @RequestBody Requests.ProviderUpsert body) {
        return dto(providers.update(id, body.displayName(), body.serviceLabel(), body.active()));
    }

    @PutMapping("/providers/{id}/availability")
    public List<ProviderAvailability> setAvailability(@PathVariable UUID id, @RequestBody List<AvailabilityInput> body) {
        return providers.replaceHours(id, body);
    }

    @PostMapping("/closures")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicClosure addClosure(@Valid @RequestBody Requests.Closure body) {
        return providers.addClosure(body.date(), body.startTime(), body.endTime(), body.reasonLabel());
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createStaff(@Valid @RequestBody Requests.Staff body) {
        UserAccount u = auth.createStaff(body.email(), body.password(), body.displayName(), body.role());
        return Map.of("id", u.getId(), "email", u.getEmail(), "role", u.getRole());
    }

    private ProviderDto dto(Provider p) {
        return new ProviderDto(p.getId(), p.getDisplayName(), p.getServiceLabel());
    }
}
