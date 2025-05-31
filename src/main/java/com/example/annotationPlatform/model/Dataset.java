package com.example.annotationPlatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Le nom du dataset est requis")
    private String name;
    private String description;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "annotators_per_text_pair")
    private Integer annotatorsPerTextPair = 1;


    private String status;
    private String sourceFilePath;
    private double completionRate;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL)
    private List<TextPair> textPairs;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL)
    private List<Category> categories;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL)
    private List<AnnotationTask> annotationTasks;

    @ManyToMany
    private List<User> assignedAnnotators;



    public void setAssignedAnnotators(List<User> annotators) {
        this.assignedAnnotators.clear();
        if (annotators != null) {
            this.assignedAnnotators.addAll(annotators);
        }
    }


    public void addAnnotator(User annotator) {
        if (!this.assignedAnnotators.contains(annotator)) {
            this.assignedAnnotators.add(annotator);
        }
    }

}

