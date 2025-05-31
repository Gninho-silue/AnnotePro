package com.example.annotationPlatform.repository;

import com.example.annotationPlatform.model.AnnotationTask;
import com.example.annotationPlatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnotationTaskRepository extends JpaRepository<AnnotationTask, Long> {
    List<AnnotationTask> findByAnnotator(User user);
    AnnotationTask getTaskById(Long taskId);
    Long countByAnnotator(User currentUser);

     @Query("SELECT COUNT(t) FROM AnnotationTask t WHERE t.annotator = :annotator AND LOWER(t.status) LIKE '%completed%'")

     Long countCompletedTasksByAnnotator(@Param("annotator") User annotator);

    List<AnnotationTask> findByDatasetId(Long datasetId);

    List<AnnotationTask> findByDatasetIdAndAnnotatorId(Long datasetId, Long userId);

    @Query("SELECT COUNT(t) FROM AnnotationTask t WHERE t.annotator = :annotator AND LOWER(t.status) LIKE '%pending%'")
    Long countPendingTasksByAnnotator(User annotator);

    @Query("SELECT t FROM AnnotationTask t WHERE t.annotator = :annotator AND t.status != :status AND t.deadline <= :deadline")
    List<AnnotationTask> findByAnnotatorAndStatusNotAndDeadlineBefore(
            @Param("annotator") User annotator,
            @Param("status") String status,
            @Param("deadline") LocalDateTime deadline);
}
