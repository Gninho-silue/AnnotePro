package com.example.annotationPlatform.controller.admin;

import com.example.annotationPlatform.exception.InvalidDatasetFormatException;
import com.example.annotationPlatform.model.Annotation;
import com.example.annotationPlatform.model.Category;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.AnnotationRepository;
import com.example.annotationPlatform.service.AnnotationService;
import com.example.annotationPlatform.service.AnnotationTaskService;
import com.example.annotationPlatform.service.DatasetService;
import com.example.annotationPlatform.service.InterAnnotatorAgreementService;
import com.example.annotationPlatform.service.UserService;
import com.example.annotationPlatform.util.AnnotatorStatsDTO;
import com.example.annotationPlatform.util.Constants;
import com.example.annotationPlatform.util.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.mail.MessagingException;

/**
 * Controller for handling admin-related operations, including dataset and user management.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('" + Constants.ROLE_ADMIN + "')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final DatasetService datasetService;
    private final UserService userService;
    private final AnnotationTaskService taskService;
    private final InterAnnotatorAgreementService agreementService;
    private final AnnotationService annotationService;
    private final AnnotationRepository annotationRepository;

    // === DASHBOARD ===

    /**
     * Displays the admin dashboard with dataset statistics and recent datasets.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the dashboard view
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        List<Dataset> datasets = datasetService.getAllDatasets();
        List<Dataset> recentDatasets = datasets.stream()
                .sorted((d1, d2) -> d2.getId().compareTo(d1.getId()))
                .limit(3)
                .collect(Collectors.toList());
        Map<String, Object> statistics = datasetService.getDatasetStatistics();

        model.addAttribute("recentDatasets", recentDatasets);
        model.addAttribute("statistics", statistics);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/dashboard";
    }

    // === DATASET MANAGEMENT ===

    /**
     * Lists all datasets with optional filtering by search term or status.
     *
     * @param search  the search term for dataset names
     * @param status  the dataset status filter
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the datasets view
     */
    @GetMapping("/datasets")
    public String datasets(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            Model model,
            HttpServletRequest request) {
        List<Dataset> datasets = datasetService.getAllDatasets();

        if (search != null && !search.trim().isEmpty()) {
            datasets = datasets.stream()
                    .filter(dataset -> dataset.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.trim().isEmpty()) {
            datasets = datasetService.getDatasetsByStatus(status);
        }

        model.addAttribute("datasets", datasets);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/datasets";
    }

    /**
     * Displays the form for creating a new dataset.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the create dataset view
     */
    @GetMapping("/datasets/create")
    public String showCreateDatasetForm(Model model, HttpServletRequest request) {
        model.addAttribute("dataset", new Dataset());
        model.addAttribute("acceptedFormats", "CSV, JSON");
        model.addAttribute("currentUser", request.getSession().getAttribute("user"));
        model.addAttribute("isEdit", false);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/create-dataset";
    }

    /**
     * Creates a new dataset from the submitted form data.
     *
     * @param dataset            the dataset details
     * @param result            the binding result for validation
     * @param file              the uploaded dataset file
     * @param classes           the semicolon-separated category names
     * @param k                 the number of annotators per text pair
     * @param model             the model for the view
     * @param redirectAttributes attributes for redirect
     * @return redirect to datasets list or back to form on error
     */
    @PostMapping("/datasets/create")
    public String createDataset(
            @ModelAttribute("dataset") @Valid Dataset dataset,
            BindingResult result,
            @RequestParam("file") MultipartFile file,
            @RequestParam("classes") String classes,
            @RequestParam("annotatorsPerTextPair") Integer k,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/create-dataset";
        }

        try {
            datasetService.createDataset(dataset, file, classes, k);
            redirectAttributes.addFlashAttribute("successMessage", "Dataset created successfully!");
            return "redirect:/admin/datasets";
        } catch (InvalidDatasetFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/create-dataset";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/create-dataset";
        }
    }

    /**
     * Displays the details of a specific dataset.
     *
     * @param id      the dataset ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the dataset details view
     */
    @GetMapping("/datasets/details/{id}")
    public String showDatasetDetails(@PathVariable Long id, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(id);
        model.addAttribute("dataset", dataset);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/dataset-details";
    }

    /**
     * Displays the form for editing a dataset.
     *
     * @param id      the dataset ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the edit dataset view
     */
    @GetMapping("/datasets/edit/{id}")
    public String showEditDatasetForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(id);
        String classesString = dataset.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.joining(";"));

        model.addAttribute("dataset", dataset);
        model.addAttribute("classes", classesString);
        model.addAttribute("isEdit", true);
        model.addAttribute("datasetId", id);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/create-dataset";
    }

    /**
     * Updates a dataset with the submitted form data.
     *
     * @param id                 the dataset ID
     * @param datasetDetails     the updated dataset details
     * @param status             the optional new status
     * @param redirectAttributes attributes for redirect
     * @return redirect to dataset details
     */
    @PostMapping("/datasets/edit/{id}")
    public String updateDataset(@PathVariable Long id,
                                @ModelAttribute Dataset datasetDetails,
                                @RequestParam(required = false) String status,
                                RedirectAttributes redirectAttributes) {
        try {
            // Get the existing dataset to preserve important fields
            Dataset existingDataset = datasetService.getDatasetById(id);
            
            // Preserve the original values
            datasetDetails.setAnnotatorsPerTextPair(existingDataset.getAnnotatorsPerTextPair());
            datasetDetails.setCategories(existingDataset.getCategories());
            datasetDetails.setSourceFilePath(existingDataset.getSourceFilePath());
            datasetDetails.setCreatedAt(existingDataset.getCreatedAt());
            
            if (status != null && !status.isEmpty()) {
                datasetDetails.setStatus(status);
            }
            
            datasetService.updateDataset(id, datasetDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Dataset mis à jour avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour du dataset : " + e.getMessage());
        }
        return "redirect:/admin/datasets";
    }

    /**
     * Updates the status of a dataset.
     *
     * @param id                 the dataset ID
     * @param status             the new status
     * @param redirectAttributes attributes for redirect
     * @return redirect to dataset details
     */
    @PostMapping("/datasets/status/{id}")
    public String updateDatasetStatus(@PathVariable Long id,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            datasetService.updateDatasetStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/datasets/details/" + id;
    }

    /**
     * Deletes a dataset.
     *
     * @param id                 the dataset ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to datasets list
     */
    @PostMapping("/datasets/delete/{id}")
    public String deleteDataset(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            datasetService.deleteDataset(id);
            redirectAttributes.addFlashAttribute("successMessage", "Dataset deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting dataset: " + e.getMessage());
        }
        return "redirect:/admin/datasets";
    }

    /**
     * Displays the form for assigning annotators to a dataset.
     *
     * @param id      the dataset ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the assign annotators view
     */
    @GetMapping("/assign/{id}")
    public String showAssignForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(id);
        List<User> users = userService.getAllAnnotators();
        model.addAttribute("datasetId", id);
        model.addAttribute("dataset", dataset);
        model.addAttribute("users", users);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/assign";
    }

    /**
     * Assigns annotators to a dataset.
     *
     * @param datasetId          the dataset ID
     * @param annotatorIds       the list of annotator IDs
     * @param redirectAttributes attributes for redirect
     * @return redirect to dataset details
     */
    @PostMapping("/assign")
    public String assignAnnotators(@RequestParam Long datasetId,
                                   @RequestParam List<Long> annotatorIds,
                                   RedirectAttributes redirectAttributes) {
        try {
            datasetService.assignAnnotators(datasetId, annotatorIds);
            redirectAttributes.addFlashAttribute("successMessage", "Annotators assigned successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning annotators: " + e.getMessage());
        }
        return "redirect:/admin/datasets/details/" + datasetId;
    }

    /**
     * Unassigns an annotator from a dataset.
     *
     * @param datasetId          the dataset ID
     * @param userId             the annotator ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to dataset details
     */
    @PostMapping("/datasets/unassign/{datasetId}/{userId}")
    public String unassignAnnotator(@PathVariable Long datasetId,
                                    @PathVariable Long userId,
                                    RedirectAttributes redirectAttributes) {
        try {
            datasetService.unassignAnnotator(datasetId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Annotator unassigned successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error unassigning annotator: " + e.getMessage());
        }
        return "redirect:/admin/datasets/details/" + datasetId;
    }

    /**
     * Displays metrics for a dataset.
     *
     * @param id      the dataset ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the metrics view
     */
    @GetMapping("/metrics/{id}")
    public String showMetrics(@PathVariable Long id, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(id);
        Map<String, Object> metrics = datasetService.calculateMetrics(id);

        model.addAttribute("dataset", dataset);
        model.addAttribute("textPairs", dataset.getTextPairs());
        model.addAttribute("metrics", metrics);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/metrics";
    }

    /**
     * Exports a dataset or its annotations to CSV or JSON.
     *
     * @param id       the dataset ID
     * @param type     the export type (dataset or annotations)
     * @param format   the export format (csv or json)
     * @param response the HTTP response
     * @throws IOException if export fails
     */
    @GetMapping("/datasets/export/{id}")
    public void exportDataset(@PathVariable Long id,
                              @RequestParam(defaultValue = "dataset") String type,
                              @RequestParam(defaultValue = "csv") String format,
                              HttpServletResponse response) throws IOException {
        Dataset dataset = datasetService.getDatasetById(id);
        byte[] data;
        String filename;
        String contentType;

        if ("annotations".equals(type)) {
            if ("json".equals(format)) {
                data = datasetService.exportAnnotationsToJson(id);
                filename = dataset.getName() + "_annotations.json";
                contentType = MediaType.APPLICATION_JSON_VALUE;
            } else {
                data = datasetService.exportAnnotationsToCsv(id);
                filename = dataset.getName() + "_annotations.csv";
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        } else {
            data = datasetService.exportDatasetToCsv(id);
            filename = dataset.getName() + ".csv";
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    /**
     * Displays statistics for annotators.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the annotator statistics view
     */
    @GetMapping("/stats/annotators")
    public String showAnnotatorStats(Model model, HttpServletRequest request) {
        List<AnnotatorStatsDTO> stats = taskService.getAnnotatorStatistics();
        model.addAttribute("stats", stats);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/annotator-stats";
    }

    /**
     * View inter-annotator agreement metrics for a dataset.
     *
     * @param datasetId the dataset ID
     * @param model the model for the view
     * @param request the HTTP request
     * @return the agreement metrics view
     */
    @GetMapping("/datasets/{datasetId}/agreement")
    public String viewAgreementMetrics(@PathVariable Long datasetId, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(datasetId);
        Map<String, Object> agreementStats = agreementService.getAgreementStatistics(datasetId);

        model.addAttribute("dataset", dataset);
        model.addAttribute("agreementStats", agreementStats);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/agreement-metrics";
    }

    /**
     * Displays all annotations for a dataset with the ability to edit them.
     *
     * @param datasetId the dataset ID
     * @param model the model for the view
     * @param request the HTTP request
     * @return the annotations management view
     */
    @GetMapping("/datasets/{datasetId}/annotations")
    public String manageAnnotations(@PathVariable Long datasetId, Model model, HttpServletRequest request) {
        Dataset dataset = datasetService.getDatasetById(datasetId);
        List<Annotation> annotations = annotationRepository.findByTextPair_DatasetId(datasetId);
        
        model.addAttribute("dataset", dataset);
        model.addAttribute("annotations", annotations);
        model.addAttribute("categories", dataset.getCategories());
        ControllerUtils.addCurrentUri(model, request);
        return "admin/manage-annotations";
    }

    /**
     * Updates an annotation.
     *
     * @param annotationId the annotation ID
     * @param categoryId the new category ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to annotations management
     */
    @PostMapping("/annotations/{annotationId}/update")
    public String updateAnnotation(@PathVariable Long annotationId,
                                 @RequestParam Long categoryId,
                                 RedirectAttributes redirectAttributes) {
        try {
            annotationService.updateAnnotation(annotationId, categoryId);
            redirectAttributes.addFlashAttribute("successMessage", "Annotation mise à jour avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour de l'annotation: " + e.getMessage());
        }
        return "redirect:/admin/datasets/" +
                annotationService.getAnnotationById(annotationId).getTextPair().getDataset().getId() + "/annotations";
    }

    /**
     * Deletes an annotation.
     *
     * @param annotationId the annotation ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to annotations management
     */
    @PostMapping("/annotations/{annotationId}/delete")
    public String deleteAnnotation(@PathVariable Long annotationId,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long datasetId = annotationService.getAnnotationById(annotationId).getTextPair().getDataset().getId();
            annotationService.deleteAnnotation(annotationId);
            redirectAttributes.addFlashAttribute("successMessage", "Annotation supprimée avec succès");
            return "redirect:/admin/datasets/" + datasetId + "/annotations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression de l'annotation: " + e.getMessage());
            return "redirect:/admin/datasets";
        }
    }

    // === USER MANAGEMENT ===

    /**
     * Lists all users with optional filtering by search term, role, or status.
     *
     * @param search  the search term for username or email
     * @param role    the role filter
     * @param status  the status filter (active/inactive)
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the users view
     */
    @GetMapping("/users")
    public String manageUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            Model model,
            HttpServletRequest request) {
        List<User> users = userService.getAllUsers();

        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(searchLower) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }
        if (role != null && !role.trim().isEmpty()) {
            users = users.stream()
                    .filter(user -> user.getRoles().stream().anyMatch(r -> r.getName().equals(role)))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.trim().isEmpty()) {
            boolean isActive = status.equals("active");
            users = users.stream()
                    .filter(user -> user.isEnabled() == isActive)
                    .collect(Collectors.toList());
        }

        model.addAttribute("users", users);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/users";
    }

    /**
     * Displays the form for creating a new user.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the create user view
     */
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model, HttpServletRequest request) {
        model.addAttribute("user", new User());
        model.addAttribute("isEdit", false);
        ControllerUtils.addCurrentUri(model, request);

        return "admin/create-user";
    }

    /**
     * Creates a new user from the submitted form data.
     *
     * @param user               the user details
     * @param result            the binding result for validation
     * @param role              the role to assign
     * @param model             the model for the view
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list or back to form on error
     */
    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             @RequestParam String role,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        logger.info("Attempting to create new user: {}", user.getUsername());
        
        if (result.hasErrors()) {
            logger.error("Validation errors occurred: {}", result.getAllErrors());
            model.addAttribute("user", user);
            ControllerUtils.addCurrentUri(model, "/admin/users/create");
            return "admin/create-user";
        }

        try {
            logger.info("Creating user with role: {}", role);
            userService.createUser(user, role);
            logger.info("User created successfully: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", 
                "Utilisateur créé avec succès. Un email contenant les identifiants a été envoyé à " + user.getEmail());
            return "redirect:/admin/users";
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email for user: {}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "L'utilisateur a été créé mais l'envoi de l'email a échoué. Veuillez contacter l'administrateur.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la création de l'utilisateur : " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Displays the form for editing a user.
     *
     * @param id      the user ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the edit user view
     */
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("isEdit", true);
        model.addAttribute("userId", id);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/create-user";
    }

    /**
     * Updates a user with the submitted form data.
     *
     * @param id                 the user ID
     * @param userDetails        the updated user details
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list
     */
    @PostMapping("/users/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            // Get the existing user to preserve the password
            User existingUser = userService.getUserById(id);
            userDetails.setPassword(existingUser.getPassword());
            userDetails.setRoles(existingUser.getRoles());
            userDetails.setEnable(existingUser.isEnabled());
            userDetails.setAccountLocked(existingUser.getAccountLocked());
            userDetails.setCreateDate(existingUser.getCreateDate());
            
            userService.updateUser(id, userDetails);
            redirectAttributes.addFlashAttribute("successMessage", "Utilisateur mis à jour avec succès");
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour de l'utilisateur : " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Toggles the enabled status of a user.
     *
     * @param id                 the user ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list
     */
    @PostMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = userService.toggleUserStatus(id);
            String status = user.isEnabled() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage", "User account " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Toggles the lock status of a user account.
     *
     * @param id                 the user ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list
     */
    @PostMapping("/users/toggle-lock/{id}")
    public String toggleUserLock(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.toggleLockStatus(id);
            String status = user.getAccountLocked() ? "locked" : "unlocked";
            redirectAttributes.addFlashAttribute("successMessage", "User account " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Deletes a user.
     *
     * @param id                 the user ID
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list
     */
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Displays the form for managing user roles.
     *
     * @param id      the user ID
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the manage roles view
     */
    @GetMapping("/users/roles/{id}")
    public String showManageRolesForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        ControllerUtils.addCurrentUri(model, request);
        return "admin/manage-roles";
    }

    /**
     * Updates the roles of a user.
     *
     * @param id                 the user ID
     * @param roleNames          the list of role names
     * @param redirectAttributes attributes for redirect
     * @return redirect to users list
     */
    @PostMapping("/users/roles/{id}")
    public String updateUserRoles(@PathVariable Long id, @RequestParam List<String> roleNames,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRoles(id, roleNames);
            redirectAttributes.addFlashAttribute("successMessage", "Roles updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating roles: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}