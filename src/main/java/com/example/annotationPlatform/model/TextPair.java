package com.example.annotationPlatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@Table(name = "text_pairs")
@NoArgsConstructor
@AllArgsConstructor
public class TextPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text1;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text2;

    @OneToMany(mappedBy = "textPair", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Annotation> annotations = new HashSet<>();

    @OneToMany(mappedBy = "textPair", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<AnnotationTask> annotationTasks = new HashSet<>();

    @Column(name = "annotations_count")
    @Builder.Default
    private Integer annotationsCount = 0;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @ManyToOne
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;
}