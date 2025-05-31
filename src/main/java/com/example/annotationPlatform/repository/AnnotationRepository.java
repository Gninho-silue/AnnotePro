package com.example.annotationPlatform.repository;

import com.example.annotationPlatform.model.Annotation;
import com.example.annotationPlatform.model.AnnotationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    Optional<Annotation> findByAnnotationTask(AnnotationTask task);

    List<Annotation> findByTextPairDatasetId(Long datasetId);

    List<Annotation> findByAnnotatorIdAndTextPairDatasetId(Long userId, Long datasetId);

    @Query("SELECT SUM(a.annotationDurationMs) FROM Annotation a WHERE a.annotator.id = :annotatorId")
    Long sumAnnotationDurationsByAnnotatorId(@Param("annotatorId") Long annotatorId);

    long countCompletedAnnotationsByAnnotatorId(Long id);

    /**
     * Find annotations by annotator ID and dataset ID.
     *
     * @param annotatorId the annotator ID
     * @param datasetId the dataset ID
     * @return list of annotations
     */
    @Query("SELECT a FROM Annotation a WHERE a.annotator.id = :annotatorId AND a.textPair.dataset.id = :datasetId")
    List<Annotation> findByAnnotatorIdAndTextPair_DatasetId(@Param("annotatorId") Long annotatorId, @Param("datasetId") Long datasetId);

    /**
     * Find all annotations for a dataset.
     *
     * @param datasetId the dataset ID
     * @return list of annotations
     */
    @Query("SELECT a FROM Annotation a WHERE a.textPair.dataset.id = :datasetId")
    List<Annotation> findByTextPair_DatasetId(@Param("datasetId") Long datasetId);
}
