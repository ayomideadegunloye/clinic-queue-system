package com.example.clinic.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    String home(Authentication auth) {
        boolean staff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECEPTIONIST") || a.getAuthority().equals("ROLE_ADMIN"));
        return staff ? "redirect:/staff/queue" : "redirect:/book";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
