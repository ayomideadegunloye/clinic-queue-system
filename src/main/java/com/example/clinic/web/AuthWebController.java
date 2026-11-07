package com.example.clinic.web;

import com.example.clinic.auth.AuthService;
import com.example.clinic.common.ApiException;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthWebController {
    private final AuthService auth;

    public AuthWebController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/register")
    String form() {
        return "register";
    }

    @PostMapping("/register")
    String register(@RequestParam String displayName, @RequestParam String email, @RequestParam String password,
                    Model model, RedirectAttributes redirect) {
        try {
            auth.registerPatient(email, password, displayName);
        } catch (ApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fieldErrors", e.getFieldErrors());
            model.addAttribute("displayName", displayName);
            model.addAttribute("email", email);
            return "register";
        }
        redirect.addFlashAttribute("success", "Account created. Please sign in.");
        return "redirect:/login";
    }
}
