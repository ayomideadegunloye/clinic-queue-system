package com.example.clinic.api;

import com.example.clinic.auth.AuthService;
import com.example.clinic.auth.UserAccount;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthService auth;

    public AuthApiController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody Requests.Register body) {
        UserAccount u = auth.registerPatient(body.email(), body.password(), body.displayName());
        return Map.of("id", u.getId(), "email", u.getEmail(), "role", u.getRole());
    }
}
