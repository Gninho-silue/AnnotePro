package com.example.annotationPlatform.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

/**
 * Utility class for common controller operations.
 */
public final class ControllerUtils {

    private ControllerUtils() {
        // Prevent instantiation
    }

    /**
     * Adds the current URI to the model.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     */
    public static void addCurrentUri(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
    }

    /**
     * Adds the current URI to the model with a specific URI.
     *
     * @param model the model for the view
     * @param uri   the URI to set
     */
    public static void addCurrentUri(Model model, String uri) {
        model.addAttribute("currentUri", uri);
    }
}