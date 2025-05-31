package com.example.annotationPlatform.repository;

import com.example.annotationPlatform.model.TextPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TextPairRepository extends JpaRepository<TextPair, Long> {
    List<TextPair> findAllByDatasetId(Long datasetId);
}
