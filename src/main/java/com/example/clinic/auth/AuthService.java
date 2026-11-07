package com.example.clinic.auth;

import com.example.clinic.common.ApiException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Deliberately generic so the response does not reveal whether an email is already registered. */
    private static final String GENERIC_REGISTRATION_ERROR =
            "Registration could not be completed. Check your details, or sign in if you already have an account.";

    private final UserAccountRepository users;
    private final PatientProfileRepository profiles;
    private final PasswordEncoder encoder;

    public AuthService(UserAccountRepository users, PatientProfileRepository profiles, PasswordEncoder encoder) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
    }

    @Transactional
    public UserAccount registerPatient(String email, String password, String displayName) {
        UserAccount user = createAccount(email, password, displayName, Role.PATIENT);
        profileFor(user.getId());
        return user;
    }

    @Transactional
    public UserAccount createStaff(String email, String password, String displayName, Role role) {
        if (role == null || role == Role.PATIENT) {
            throw ApiException.validation("role", "Staff role must be RECEPTIONIST or ADMIN.");
        }
        return createAccount(email, password, displayName, role);
    }

    private UserAccount createAccount(String email, String password, String displayName, Role role) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) {
            throw ApiException.validation("email", "Enter a valid email address.");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw ApiException.validation("password", "Password must be 8 to 72 characters.");
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > 100) {
            throw ApiException.validation("displayName", "Name is required (max 100 characters).");
        }
        if (users.existsByEmail(normalized)) {
            throw genericRegistrationError();
        }
        UserAccount user = new UserAccount();
        user.setEmail(normalized);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw genericRegistrationError();
        }
        PatientProfile profile = new PatientProfile();
        profile.setUserId(user.getId());
        profile.setDisplayName(displayName.trim());
        profiles.save(profile);
        return user;
    }

    /** Returns the patient profile for a user, creating a minimal one if needed (e.g. admin booking for support). */
    @Transactional
    public PatientProfile profileFor(UUID userId) {
        return profiles.findByUserId(userId).orElseGet(() -> {
            UserAccount user = users.findById(userId)
                    .orElseThrow(() -> ApiException.notFound("User not found."));
            PatientProfile p = new PatientProfile();
            p.setUserId(userId);
            p.setDisplayName(user.getEmail());
            return profiles.save(p);
        });
    }

    private ApiException genericRegistrationError() {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", GENERIC_REGISTRATION_ERROR, Map.of());
    }
}
