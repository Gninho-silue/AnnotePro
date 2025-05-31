package com.example.annotationPlatform.controller.auth;

import com.example.annotationPlatform.util.Constants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for handling authentication-related operations, including login and redirection.
 */
@Controller
public class AuthController {

    /**
     * Displays the login page with optional error or status messages.
     *
     * @param error   indicates if there was a login error
     * @param logout  indicates if the user logged out
     * @param expired indicates if the session expired
     * @param model   the model for the view
     * @return the login view
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "expired", required = false) String expired,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        if (expired != null) {
            model.addAttribute("expired", "Your session has expired. Please log in again");
        }
        return "login";
    }

    /**
     * Redirects authenticated users to their respective dashboards based on roles.
     *
     * @return redirect to the appropriate dashboard or login page
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "redirect:/login";
        }

        if (auth.getAuthorities().contains(new SimpleGrantedAuthority(Constants.ROLE_ADMIN))) {
            return "redirect:/admin/dashboard";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority(Constants.ROLE_ANNOTATOR))) {
            return "redirect:/annotator/dashboard";
        }
        return "redirect:/login";
    }

    /**
     * Displays the access denied page.
     *
     * @return the access denied view
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}