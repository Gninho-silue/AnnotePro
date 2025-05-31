package com.example.annotationPlatform.repository;

import com.example.annotationPlatform.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
