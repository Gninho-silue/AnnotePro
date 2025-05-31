package com.example.annotationPlatform.controller;

import com.example.annotationPlatform.exception.OperationNonPermitException;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.service.UserService;
import com.example.annotationPlatform.util.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling user profile-related operations.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;

    /**
     * Displays the user's profile page.
     *
     * @param model the model for the view
     * @param request the HTTP request
     * @param authentication the current user's authentication
     * @return the profile view
     */
    @GetMapping
    public String showProfile(Model model, HttpServletRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        model.addAttribute("user", user);
        ControllerUtils.addCurrentUri(model, request);
        return "profile";
    }

    /**
     * Updates the user's profile information.
     *
     * @param userDetails the updated user details
     * @param result the binding result for validation
     * @param authentication the current user's authentication
     * @param redirectAttributes attributes for redirect
     * @return redirect to profile page
     */
    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("user") User userDetails,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            logger.error("Validation errors occurred while updating profile: {}", result.getAllErrors());
            return "profile";
        }

        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            // Update only allowed fields
            
            currentUser.setEmail(userDetails.getEmail());
            currentUser.setFirstName(userDetails.getFirstName());
            currentUser.setLastName(userDetails.getLastName());
            
            userService.updateUser(currentUser.getId(), currentUser);
            
            logger.info("Profile updated successfully for user: {}", username);
            redirectAttributes.addFlashAttribute("successMessage", "Profil mis à jour avec succès");
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la mise à jour du profil : " + e.getMessage());
        }
        
        return "redirect:/profile";
    }

    /**
     * Changes the user's password.
     *
     * @param currentPassword the user's current password
     * @param newPassword the new password
     * @param confirmPassword the password confirmation
     * @param authentication the current user's authentication
     * @param redirectAttributes attributes for redirect
     * @return redirect to profile page
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new OperationNonPermitException("Les mots de passe ne correspondent pas");
            }

            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            userService.changePassword(user.getId(), currentPassword, newPassword);
            
            logger.info("Password changed successfully for user: {}", username);
            redirectAttributes.addFlashAttribute("successMessage", "Mot de passe modifié avec succès");
        } catch (OperationNonPermitException e) {
            logger.error("Password change failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Error changing password: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors du changement de mot de passe : " + e.getMessage());
        }
        
        return "redirect:/profile";
    }
} 