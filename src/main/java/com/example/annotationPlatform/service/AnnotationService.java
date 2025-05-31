package com.example.annotationPlatform.service;

import com.example.annotationPlatform.exception.OperationNonPermitException;
import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.model.Annotation;
import com.example.annotationPlatform.model.AnnotationTask;
import com.example.annotationPlatform.model.Category;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.AnnotationRepository;
import com.example.annotationPlatform.repository.AnnotationTaskRepository;
import com.example.annotationPlatform.repository.CategoryRepository;
import com.example.annotationPlatform.repository.UserRepository;
import com.example.annotationPlatform.util.Constants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for handling annotation-related operations.
 */
@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final AnnotationTaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DatasetService datasetService;

    /**
     * Saves an annotation for a given task and category, updating task status and annotator statistics.
     *
     * @param taskId     ID of the annotation task
     * @param categoryId ID of the category to associate with the annotation
     * @throws RessourceNotFoundException if task or category is not found
     * @throws OperationNonPermitException if task is already completed
     */
    @Transactional
    public void saveAnnotation(Long taskId, Long categoryId) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RessourceNotFoundException("Task not found with ID: " + taskId));

        if (Constants.TASK_STATUS_COMPLETED.equals(task.getStatus())) {
            throw new OperationNonPermitException("Task is already completed.");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RessourceNotFoundException("Category not found with ID: " + categoryId));

        LocalDateTime now = LocalDateTime.now();
        long durationMs = java.time.Duration.between(task.getAssignedAt(), now).toMillis();

        // Set task status to IN_PROGRESS if it was PENDING
        if (Constants.TASK_STATUS_PENDING.equals(task.getStatus())) {
            task.setStatus(Constants.TASK_STATUS_IN_PROGRESS);
            task.setStartedAt(now);
            taskRepository.save(task);
        }

        Annotation annotation = annotationRepository.findByAnnotationTask(task).orElse(new Annotation());
        annotation.setAnnotationTask(task);
        annotation.setAnnotator(task.getAnnotator());
        annotation.setTextPair(task.getTextPair());
        annotation.setCategory(category);
        annotation.setCreatedAt(now);
        annotation.setAnnotationTime(now);
        annotation.setAnnotationDurationMs(durationMs);
        annotation.setStatus(Constants.ANNOTATION_STATUS_ANNOTATED);

        annotationRepository.save(annotation);

        // Update task status to COMPLETED
        task.setStatus(Constants.TASK_STATUS_COMPLETED);
        task.setCompletedAt(now);
        taskRepository.save(task);

        // Update annotator statistics
        updateAnnotatorStatistics(task.getAnnotator());

        // Update dataset completion rate
        datasetService.calculateCompletionRate(task.getDataset());
    }

    /**
     * Updates the annotator's statistics based on their completed annotations.
     *
     * @param annotator the user whose statistics need to be updated
     */
    private void updateAnnotatorStatistics(User annotator) {
        Long totalDuration = annotationRepository.sumAnnotationDurationsByAnnotatorId(annotator.getId());
        long count = annotationRepository.countCompletedAnnotationsByAnnotatorId(annotator.getId());

        if (count > 0) {
            long avgDuration = totalDuration / count;
            annotator.setAvgAnnotationTimeMs(avgDuration);
            annotator.setAnnotationsCount((int) count);
            userRepository.save(annotator);
        }
    }

    /**
     * Deletes an annotation and updates the associated task and dataset.
     *
     * @param annotationId ID of the annotation to delete
     * @throws RessourceNotFoundException if annotation not found
     */
    @Transactional
    public void deleteAnnotation(Long annotationId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new RessourceNotFoundException("Annotation not found with ID: " + annotationId));

        // Get the dataset before deleting the annotation
        Dataset dataset = annotation.getTextPair().getDataset();

        // Update the task status
        AnnotationTask task = annotation.getAnnotationTask();
        task.setStatus(Constants.TASK_STATUS_PENDING);
        task.setCompletedAt(null);
        taskRepository.save(task);

        // Delete the annotation
        annotationRepository.delete(annotation);

        // Update annotator statistics
        updateAnnotatorStatistics(annotation.getAnnotator());

        // Update dataset completion rate
        datasetService.calculateCompletionRate(dataset);
    }

    /**
     * Retrieves an annotation by its ID.
     *
     * @param annotationId the annotation ID
     * @return the annotation
     * @throws RessourceNotFoundException if annotation not found
     */
    public Annotation getAnnotationById(Long annotationId) {
        return annotationRepository.findById(annotationId)
                .orElseThrow(() -> new RessourceNotFoundException("Annotation not found with ID: " + annotationId));
    }

    /**
     * Updates an annotation with a new category.
     *
     * @param annotationId the annotation ID
     * @param categoryId the new category ID
     * @throws RessourceNotFoundException if annotation or category not found
     */
    @Transactional
    public void updateAnnotation(Long annotationId, Long categoryId) {
        Annotation annotation = getAnnotationById(annotationId);
        Category newCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RessourceNotFoundException("Category not found with ID: " + categoryId));

        annotation.setCategory(newCategory);
        annotation.setUpdatedAt(LocalDateTime.now());
        annotationRepository.save(annotation);

        // Update annotator statistics
        updateAnnotatorStatistics(annotation.getAnnotator());

        // Update dataset completion rate
        datasetService.calculateCompletionRate(annotation.getTextPair().getDataset());
    }
}