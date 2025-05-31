package com.example.annotationPlatform.controller.annotator;

import com.example.annotationPlatform.model.AnnotationTask;
import com.example.annotationPlatform.model.Category;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.service.AnnotationService;
import com.example.annotationPlatform.service.AnnotationTaskService;
import com.example.annotationPlatform.util.AnnotatorStatsDTO;
import com.example.annotationPlatform.util.Constants;
import com.example.annotationPlatform.util.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

/**
 * Controller for handling annotator-related operations, including task management and statistics.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/annotator")
@PreAuthorize("hasRole('" + Constants.ROLE_ANNOTATOR + "')")
public class AnnotatorController {

    private final AnnotationTaskService taskService;
    private final AnnotationService annotationService;

    /**
     * Displays the annotator dashboard with task statistics.
     *
     * @param model         the model for the view
     * @param authentication the current authentication
     * @param request       the HTTP request
     * @return the dashboard view
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication, HttpServletRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        Map<Dataset, List<AnnotationTask>> recentTasksByDataset = taskService.getRecentTasksByDatasetForCurrentUser();
        
        model.addAttribute("userFirstName", currentUser.getFirstName());
        model.addAttribute("pendingTasksCount", taskService.countPendingTasksForCurrentUser());
        model.addAttribute("completedAnnotationsCount", currentUser.getAnnotationsCount());
        model.addAttribute("recentTasksByDataset", recentTasksByDataset);
        ControllerUtils.addCurrentUri(model, request);
        return "annotator/dashboard";
    }

    /**
     * Lists all tasks assigned to the current annotator.
     *
     * @param model   the model for the view
     * @param request the HTTP request
     * @return the tasks view
     */
    @GetMapping("/tasks")
    public String tasks(Model model, HttpServletRequest request) {
        Map<Dataset, List<AnnotationTask>> tasksByDataset = taskService.getTasksByDatasetForCurrentUser();
        List<AnnotationTask> urgentTasks = tasksByDataset.values().stream()
                .flatMap(List::stream)
                .filter(t -> t.getDeadline() != null && !Objects.equals(t.getStatus(), Constants.TASK_STATUS_COMPLETED)
                        && t.getDeadline().isBefore(LocalDateTime.now().plusHours(24)))
                .toList();
        model.addAttribute("tasks", tasksByDataset);
        model.addAttribute("urgentTasks", urgentTasks);
        ControllerUtils.addCurrentUri(model, request);
        return "annotator/tasks";
    }

    /**
     * Displays the form for annotating a task.
     *
     * @param taskId the task ID
     * @param model  the model for the view
     * @return the annotate view
     */
    @GetMapping("/annotate/{taskId}")
    public String annotate(@PathVariable Long taskId, Model model) {
        AnnotationTask task = taskService.getTaskById(taskId);
        List<Category> categories = task.getTextPair().getDataset().getCategories();
        model.addAttribute("task", task);
        model.addAttribute("categories", categories);
        return "annotator/annotate";
    }

    /**
     * Saves an annotation for a task.
     *
     * @param taskId     the task ID
     * @param categoryId the category ID
     * @param redirectToNext whether to redirect to the next task
     * @return redirect to next task or tasks list
     */
    @PostMapping("/saveAnnotation")
    public String saveAnnotation(@RequestParam Long taskId, 
                               @RequestParam Long categoryId,
                               @RequestParam(required = false, defaultValue = "false") boolean redirectToNext) {
        annotationService.saveAnnotation(taskId, categoryId);
        
        if (redirectToNext) {
            // Get the current task to find its dataset
            AnnotationTask currentTask = taskService.getTaskById(taskId);
            Dataset dataset = currentTask.getDataset();
            
            // Get the next pending task in the same dataset
            List<AnnotationTask> nextTasks = taskService.getTasksForCurrentUser().stream()
                .filter(t -> t.getDataset().getId().equals(dataset.getId()))
                .filter(t -> Constants.TASK_STATUS_PENDING.equals(t.getStatus()))
                .toList();
            
            if (!nextTasks.isEmpty()) {
                return "redirect:/annotator/annotate/" + nextTasks.get(0).getId();
            }
        }
        
        return "redirect:/annotator/tasks";
    }

    /**
     * Displays statistics for the current annotator.
     *
     * @param model         the model for the view
     * @param authentication the current authentication
     * @param request       the HTTP request
     * @return the statistics view
     */
    @GetMapping("/stats")
    public String stats(Model model, Authentication authentication, HttpServletRequest request) {
        User currentUser = (User) authentication.getPrincipal();
        List<AnnotatorStatsDTO> allStats = taskService.getAnnotatorStatistics();
        AnnotatorStatsDTO userStats = allStats.stream()
                .filter(stat -> stat.userId().equals(currentUser.getId()))
                .findFirst()
                .orElse(new AnnotatorStatsDTO(
                        currentUser.getId(),
                        currentUser.getUsername(),
                        currentUser.getEmail(),
                        0L, 0L, 0L, 0L,
                        new HashMap<>()
                ));

        model.addAttribute("stats", userStats);
        ControllerUtils.addCurrentUri(model, request);
        return "annotator/stats";
    }

    /**
     * Navigates to the next task in the dataset.
     *
     * @param currentTaskId the current task ID
     * @return redirect to the next task or back to tasks list if no more tasks
     */
    @GetMapping("/annotate/{taskId}/next")
    public String nextTask(@PathVariable("taskId") Long currentTaskId) {
        AnnotationTask nextTask = taskService.getNextTaskInDataset(currentTaskId);
        if (nextTask != null) {
            return "redirect:/annotator/annotate/" + nextTask.getId();
        }
        return "redirect:/annotator/tasks";
    }

    /**
     * Navigates to the previous task in the dataset.
     *
     * @param currentTaskId the current task ID
     * @return redirect to the previous task or back to tasks list if no previous tasks
     */
    @GetMapping("/annotate/{taskId}/previous")
    public String previousTask(@PathVariable("taskId") Long currentTaskId) {
        AnnotationTask previousTask = taskService.getPreviousTaskInDataset(currentTaskId);
        if (previousTask != null) {
            return "redirect:/annotator/annotate/" + previousTask.getId();
        }
        return "redirect:/annotator/tasks";
    }
}