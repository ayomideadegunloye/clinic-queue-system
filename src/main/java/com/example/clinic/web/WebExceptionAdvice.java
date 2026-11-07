package com.example.clinic.web;

import com.example.clinic.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Turns business-rule failures from form posts into a flash message on the page the user came from. */
@ControllerAdvice(basePackages = "com.example.clinic.web")
public class WebExceptionAdvice {

    @ExceptionHandler(ApiException.class)
    String handle(ApiException e, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", e.getMessage());
        return "redirect:" + backTarget(request.getHeader("Referer"));
    }

    /** Only the path and query of the referer are used, so the redirect can never leave this site. */
    private static String backTarget(String referer) {
        if (referer == null) return "/";
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) return "/";
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }
}
