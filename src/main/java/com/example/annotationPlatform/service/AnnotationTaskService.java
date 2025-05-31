package com.example.annotationPlatform.service;

import com.example.annotationPlatform.model.AnnotationTask;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.TextPair;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.AnnotationRepository;
import com.example.annotationPlatform.repository.AnnotationTaskRepository;
import com.example.annotationPlatform.repository.TextPairRepository;
import com.example.annotationPlatform.repository.UserRepository;
import com.example.annotationPlatform.util.Constants;
import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.util.AnnotatorStatsDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing annotation tasks, including task creation and statistics.
 */
@Service
@RequiredArgsConstructor
public class AnnotationTaskService {

    private final AnnotationTaskRepository taskRepository;
    private final UserService userService;
    private final TextPairRepository textPairRepository;
    private final AnnotationRepository annotationRepository;
    private final UserRepository userRepository;

    private final Logger logger = Logger.getLogger(AnnotationTaskService.class.getName());

    /**
     * Retrieves tasks assigned to the current user.
     *
     * @return list of tasks for the current user
     */
    public List<AnnotationTask> getTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        return taskRepository.findByAnnotator(currentUser);
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param taskId the task ID
     * @return the annotation task
     * @throws RessourceNotFoundException if task not found
     */
    public AnnotationTask getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RessourceNotFoundException("Annotation task not found with ID: " + taskId));
    }

    /**
     * Creates annotation tasks for a dataset, ensuring each text pair is assigned to K annotators.
     * When K=1, text pairs are distributed evenly among annotators.
     *
     * @param dataset         the dataset
     * @param validAnnotators list of valid annotators
     * @throws IllegalArgumentException if insufficient annotators
     */
    @Transactional
    public void createAnnotationTasksForDataset(Dataset dataset, List<User> validAnnotators) {
        List<TextPair> textPairs = textPairRepository.findAllByDatasetId(dataset.getId());
        int K = dataset.getAnnotatorsPerTextPair() != null ? dataset.getAnnotatorsPerTextPair() : 1;

        validateAnnotators(validAnnotators, K);

        List<AnnotationTask> newTasks = new ArrayList<>();
        Random random = new Random();

        if (!textPairs.isEmpty()) {
            // Get existing tasks for this dataset
            List<AnnotationTask> existingTasks = taskRepository.findByDatasetId(dataset.getId());
            
            // Create a map of text pair ID to list of assigned annotator IDs
            Map<Long, List<Long>> textPairAssignments = new HashMap<>();
            for (AnnotationTask task : existingTasks) {
                textPairAssignments.computeIfAbsent(task.getTextPair().getId(), k -> new ArrayList<>())
                        .add(task.getAnnotator().getId());
            }

            if (K == 1) {
                // For K=1, distribute text pairs evenly among annotators
                List<TextPair> unassignedPairs = textPairs.stream()
                        .filter(pair -> !textPairAssignments.containsKey(pair.getId()) || 
                                textPairAssignments.get(pair.getId()).isEmpty())
                        .toList();

                if (!unassignedPairs.isEmpty()) {
                    // Calculate tasks per annotator
                    int totalPairs = unassignedPairs.size();
                    int basePairsPerAnnotator = totalPairs / validAnnotators.size();
                    int extraPairs = totalPairs % validAnnotators.size();

                    // Shuffle annotators for random distribution
                    List<User> shuffledAnnotators = new ArrayList<>(validAnnotators);
                    Collections.shuffle(shuffledAnnotators, random);

                    int pairIndex = 0;
                    for (User annotator : shuffledAnnotators) {
                        // Calculate how many pairs this annotator should get
                        int pairsForThisAnnotator = basePairsPerAnnotator + (extraPairs > 0 ? 1 : 0);
                        extraPairs--;

                        // Assign pairs to this annotator
                        for (int i = 0; i < pairsForThisAnnotator && pairIndex < unassignedPairs.size(); i++) {
                            TextPair textPair = unassignedPairs.get(pairIndex++);
                            createTask(dataset, newTasks, annotator, textPair);
                        }
                    }
                }
            } else {
                // For K>1, use the original logic
                for (TextPair textPair : textPairs) {
                    List<Long> assignedAnnotators = textPairAssignments.getOrDefault(textPair.getId(), new ArrayList<>());
                    int remainingAssignments = K - assignedAnnotators.size();

                    if (remainingAssignments > 0) {
                        List<User> availableAnnotators = validAnnotators.stream()
                                .filter(a -> !assignedAnnotators.contains(a.getId()))
                                .collect(Collectors.toList());

                        Collections.shuffle(availableAnnotators, random);

                        for (int i = 0; i < Math.min(remainingAssignments, availableAnnotators.size()); i++) {
                            User annotator = availableAnnotators.get(i);
                            createTask(dataset, newTasks, annotator, textPair);
                        }
                    }
                }
            }

            // Save all new tasks
            if (!newTasks.isEmpty()) {
                taskRepository.saveAll(newTasks);
            }
        }
    }

    private void createTask(Dataset dataset, List<AnnotationTask> newTasks, User annotator, TextPair textPair) {
        AnnotationTask task = new AnnotationTask();
        task.setTextPair(textPair);
        task.setAnnotator(annotator);
        task.setDataset(dataset);
        task.setStatus(Constants.TASK_STATUS_PENDING);
        task.setAssignedAt(LocalDateTime.now());
        task.setDeadline(LocalDateTime.now().plusHours(72));
        newTasks.add(task);
    }

    /**
     * Retrieves statistics for all annotators.
     *
     * @return list of annotator statistics
     */
    public List<AnnotatorStatsDTO> getAnnotatorStatistics() {
        List<User> annotators = userRepository.findByRoles_Name(Constants.ROLE_ANNOTATOR);

        return annotators.stream().map(user -> {
            long totalTasks = taskRepository.countByAnnotator(user);
            long completedTasks = annotationRepository.countCompletedAnnotationsByAnnotatorId(user.getId());
            long totalAnnotations = user.getAnnotationsCount();
            long totalDuration = user.getAvgAnnotationTimeMs();

            // Calculate dataset progress
            Map<Long, AnnotatorStatsDTO.DatasetProgress> datasetProgress = new HashMap<>();
            List<AnnotationTask> userTasks = taskRepository.findByAnnotator(user);
            Map<Dataset, List<AnnotationTask>> tasksByDataset = userTasks.stream()
                    .collect(Collectors.groupingBy(AnnotationTask::getDataset));

            tasksByDataset.forEach((dataset, tasks) -> {
                long datasetTotalTasks = tasks.size();
                long datasetCompletedTasks = tasks.stream()
                        .filter(t -> Constants.TASK_STATUS_COMPLETED.equals(t.getStatus()))
                        .count();
                datasetProgress.put(dataset.getId(), new AnnotatorStatsDTO.DatasetProgress(
                        dataset.getName(),
                        datasetTotalTasks,
                        datasetCompletedTasks
                ));
            });

            return new AnnotatorStatsDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    totalTasks,
                    completedTasks,
                    totalAnnotations,
                    totalDuration,
                    datasetProgress
            );
        }).collect(Collectors.toList());
    }

    /**
     * Counts total tasks for the current user.
     *
     * @return number of tasks
     */
    public Long countTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        return taskRepository.countByAnnotator(currentUser);
    }

    /**
     * Counts pending tasks for the current user.
     *
     * @return number of pending tasks
     */
    public Long countPendingTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        return taskRepository.countPendingTasksByAnnotator(currentUser);
    }

    /**
     * Counts completed tasks for the current user.
     *
     * @return number of completed tasks
     */
    public Long countCompletedTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        return taskRepository.countCompletedTasksByAnnotator(currentUser);
    }

    /**
     * Validates that there are enough annotators for the required assignments.
     *
     * @param annotators list of annotators
     * @param required   minimum number of annotators required
     * @throws IllegalArgumentException if insufficient annotators
     */
    private void validateAnnotators(List<User> annotators, int required) {
        if (annotators.size() < required) {
            throw new IllegalArgumentException("At least " + required + " annotators are required for assignment");
        }
    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return the current user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.getUserByUsername(auth.getName());
    }

    /**
     * Retrieves tasks grouped by dataset for the current user.
     *
     * @return map of datasets to their tasks
     */
    public Map<Dataset, List<AnnotationTask>> getTasksByDatasetForCurrentUser() {
        User currentUser = getCurrentUser();
        List<AnnotationTask> tasks = taskRepository.findByAnnotator(currentUser);
        return tasks.stream()
                .collect(Collectors.groupingBy(AnnotationTask::getDataset));
    }

    /**
     * Retrieves recent tasks grouped by dataset for the current user.
     * Recent tasks are defined as tasks that are not completed and have a deadline within the next 7 days.
     *
     * @return map of datasets to their recent tasks
     */
    public Map<Dataset, List<AnnotationTask>> getRecentTasksByDatasetForCurrentUser() {
        User currentUser = getCurrentUser();
        LocalDateTime weekFromNow = LocalDateTime.now().plusDays(7);
        List<AnnotationTask> tasks = taskRepository.findByAnnotatorAndStatusNotAndDeadlineBefore(
                currentUser, Constants.TASK_STATUS_COMPLETED, weekFromNow);
        return tasks.stream()
                .collect(Collectors.groupingBy(AnnotationTask::getDataset));
    }

    /**
     * Gets the next task in the dataset for the current user.
     *
     * @param currentTaskId the current task ID
     * @return the next task, or null if no more tasks
     */
    public AnnotationTask getNextTaskInDataset(Long currentTaskId) {
        AnnotationTask currentTask = getTaskById(currentTaskId);
        User currentUser = getCurrentUser();
        List<AnnotationTask> datasetTasks = taskRepository.findByDatasetIdAndAnnotatorId(
                currentTask.getDataset().getId(), currentUser.getId());
        
        return datasetTasks.stream()
                .filter(task -> task.getId() > currentTaskId)
                .min(Comparator.comparing(AnnotationTask::getId))
                .orElse(null);
    }

    /**
     * Gets the previous task in the dataset for the current user.
     *
     * @param currentTaskId the current task ID
     * @return the previous task, or null if no previous tasks
     */
    public AnnotationTask getPreviousTaskInDataset(Long currentTaskId) {
        AnnotationTask currentTask = getTaskById(currentTaskId);
        User currentUser = getCurrentUser();
        List<AnnotationTask> datasetTasks = taskRepository.findByDatasetIdAndAnnotatorId(
                currentTask.getDataset().getId(), currentUser.getId());
        
        return datasetTasks.stream()
                .filter(task -> task.getId() < currentTaskId)
                .max(Comparator.comparing(AnnotationTask::getId))
                .orElse(null);
    }
}