package com.example.clinic.api;

import com.example.clinic.auth.Role;
import com.example.clinic.queue.VisitStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Request bodies for the REST API. */
public final class Requests {
    private Requests() {}

    public record Register(@NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(min = 8, max = 72) String password,
                           @NotBlank @Size(max = 100) String displayName) {}

    public record Book(@NotNull UUID providerId, @NotNull Instant startAt) {}

    public record WalkIn(@NotBlank @Size(max = 100) String displayName,
                         @Size(max = 100) String contact,
                         @NotNull UUID providerId) {}

    public record Status(@NotNull VisitStatus status) {}

    public record ProviderUpsert(@NotBlank @Size(max = 100) String displayName,
                                 @NotBlank @Size(max = 100) String serviceLabel,
                                 Boolean active) {}

    public record Closure(@NotNull LocalDate date, LocalTime startTime, LocalTime endTime,
                          @Size(max = 100) String reasonLabel) {}

    public record Staff(@NotBlank @Email @Size(max = 254) String email,
                        @NotBlank @Size(min = 8, max = 72) String password,
                        @NotBlank @Size(max = 100) String displayName,
                        @NotNull Role role) {}
}
