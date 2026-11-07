package com.example.clinic.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Authenticated principal that carries the user id and role. */
public class AppUser extends User {
    private final UUID id;
    private final Role role;

    public AppUser(UserAccount account) {
        super(account.getEmail(), account.getPasswordHash(), account.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        this.id = account.getId();
        this.role = account.getRole();
    }

    public UUID getId() { return id; }
    public Role getRole() { return role; }
}
