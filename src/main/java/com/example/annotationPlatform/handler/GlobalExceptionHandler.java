package com.example.annotationPlatform.handler;

import com.example.annotationPlatform.exception.InvalidDatasetFormatException;
import com.example.annotationPlatform.exception.OperationNonPermitException;
import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.util.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for managing application-wide exceptions and displaying error messages in views.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles RessourceNotFoundException by displaying an error message in the view.
     *
     * @param ex      the exception
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the error view or redirect to the previous page
     */
    @ExceptionHandler(RessourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleRessourceNotFoundException(RessourceNotFoundException ex, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.warn("Resource not found: {}", ex.getMessage());
        String referrer = request.getHeader("Referer");
        if (referrer != null && !referrer.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + getPathFromReferrer(referrer);
        }
        model.addAttribute("errorMessage", ex.getMessage());
        ControllerUtils.addCurrentUri(model, request);
        return "error/generic";
    }

    /**
     * Handles InvalidDatasetFormatException by displaying an error message in the create dataset view.
     *
     * @param ex      the exception
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the created dataset view or redirect
     */
    @ExceptionHandler(InvalidDatasetFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidDatasetFormatException(InvalidDatasetFormatException ex, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.warn("Invalid dataset format: {}", ex.getMessage());
        String referrer = request.getHeader("Referer");
        if (referrer != null && referrer.contains("/admin/datasets/create")) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("dataset", new com.example.annotationPlatform.model.Dataset());
            model.addAttribute("acceptedFormats", "CSV, JSON");
            ControllerUtils.addCurrentUri(model, "/admin/datasets/create");
            return "admin/create-dataset";
        }
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/datasets";
    }

    /**
     * Handles OperationNonPermitException by displaying an error message in the view.
     *
     * @param ex      the exception
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the error view or redirect to the previous page
     */
    @ExceptionHandler(OperationNonPermitException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleOperationNonPermitException(OperationNonPermitException ex, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.warn("Operation not permitted: {}", ex.getMessage());
        String referrer = request.getHeader("Referer");
        if (referrer != null && !referrer.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + getPathFromReferrer(referrer);
        }
        model.addAttribute("errorMessage", ex.getMessage());
        ControllerUtils.addCurrentUri(model, request);
        return "error/error";
    }

    /**
     * Handles generic exceptions by displaying a generic error message.
     *
     * @param ex      the exception
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the generic error view
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        String referrer = request.getHeader("Referer");
        if (referrer != null && !referrer.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
            return "redirect:" + getPathFromReferrer(referrer);
        }
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        ControllerUtils.addCurrentUri(model, request);
        return "error/error";
    }

    /**
     * Extracts the path from the referrer URL for redirection.
     *
     * @param referrer the referrer URL
     * @return the path for redirection
     */
    private String getPathFromReferrer(String referrer) {
        try {
            return java.net.URI.create(referrer).toURL().getPath();
        } catch (java.net.MalformedURLException e) {
            logger.warn("Invalid referrer URL: {}", referrer);
            return "/admin/dashboard";
        }
    }
}