package com.example.annotationPlatform.repository;

import com.example.annotationPlatform.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByStatus(String status);

    Integer countByStatus(String status);
}
