package com.example.annotationPlatform.service;

import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.model.TextPair;
import com.example.annotationPlatform.repository.TextPairRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing text pair-related operations.
 */
@Service
@RequiredArgsConstructor
public class TextPairService {

    private final TextPairRepository textPairRepository;

    /**
     * Retrieves all text pairs.
     *
     * @return list of text pairs
     */
    public List<TextPair> getAllTextPairs() {
        return textPairRepository.findAll();
    }

    /**
     * Retrieves a text pair by its ID.
     *
     * @param id the text pair ID
     * @return the text pair
     * @throws RessourceNotFoundException if text pair not found
     */
    public TextPair getTextPairById(Long id) {
        return textPairRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Text pair not found with ID: " + id));
    }

    /**
     * Creates a new text pair.
     *
     * @param textPair the text pair to create
     * @return the created text pair
     */
    @Transactional
    public TextPair createTextPair(TextPair textPair) {
        return textPairRepository.save(textPair);
    }

    /**
     * Updates an existing text pair.
     *
     * @param id             the text pair ID
     * @param textPairDetails the updated text pair details
     * @return the updated text pair
     * @throws RessourceNotFoundException if a text pair not found
     */

    @Transactional
    public TextPair updateTextPair(Long id, TextPair textPairDetails) {
        TextPair textPair = getTextPairById(id);
        textPair.setText1(textPairDetails.getText1());
        textPair.setText2(textPairDetails.getText2());
        textPair.setDataset(textPairDetails.getDataset());
        return textPairRepository.save(textPair);
    }

    /**
     * Deletes a text pair by its ID.
     *
     * @param id the text pair ID
     * @throws RessourceNotFoundException if a text pair not found
     */
    @Transactional
    public void deleteTextPair(Long id) {
        TextPair textPair = getTextPairById(id);
        textPairRepository.delete(textPair);
    }
}