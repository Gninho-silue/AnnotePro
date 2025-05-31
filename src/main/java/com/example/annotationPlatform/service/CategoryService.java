package com.example.annotationPlatform.service;

import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.model.Category;
import com.example.annotationPlatform.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing category-related operations.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Retrieves all categories.
     *
     * @return list of categories
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Retrieves a category by its ID.
     *
     * @param id the category ID
     * @return the category
     * @throws RessourceNotFoundException if category not found
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Category not found with ID: " + id));
    }

    /**
     * Creates a new category.
     *
     * @param category the category to create
     * @return the created category
     */
    @Transactional
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    /**
     * Updates an existing category.
     *
     * @param id             the category ID
     * @param categoryDetails the updated category details
     * @return the updated category
     * @throws RessourceNotFoundException if category not found
     */
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        category.setName(categoryDetails.getName());
        category.setDataset(categoryDetails.getDataset());
        return categoryRepository.save(category);
    }

    /**
     * Deletes a category by its ID.
     *
     * @param id the category ID
     * @throws RessourceNotFoundException if category not found
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}