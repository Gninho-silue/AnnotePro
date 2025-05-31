package com.example.annotationPlatform.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "annotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    LocalDateTime createdAt;
    @LastModifiedDate
    LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "text_pair_id", nullable = false)
    private TextPair textPair;

    private String status;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "annotator_id", nullable = false)
    private User annotator;

    @OneToOne
    @JoinColumn(name = "annotation_task_id", nullable = false)
    private AnnotationTask annotationTask;

    @Column(name = "annotation_time", nullable = false)
    private LocalDateTime annotationTime;

    @Column(name = "annotation_duration_ms")
    private Long annotationDurationMs;

    @Column(name = "is_validated")
    private Boolean isValidated = false;

    @Column(name = "validation_date")
    private LocalDateTime validationDate;


}