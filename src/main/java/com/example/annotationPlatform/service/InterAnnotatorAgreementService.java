package com.example.annotationPlatform.service;

import com.example.annotationPlatform.model.Annotation;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.TextPair;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.AnnotationRepository;
import com.example.annotationPlatform.repository.DatasetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterAnnotatorAgreementService {

    private final DatasetRepository datasetRepository;
    private final AnnotationRepository annotationRepository;

    /**
     * Calculate Cohen's Kappa for a pair of annotators on a dataset.
     * Cohen's Kappa is used when there are exactly two annotators.
     *
     * @param datasetId the dataset ID
     * @param annotator1Id the first annotator's ID
     * @param annotator2Id the second annotator's ID
     * @return the Cohen's Kappa score
     */
    public double calculateCohensKappa(Long datasetId, Long annotator1Id, Long annotator2Id) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));

        // Get all text pairs in the dataset
        List<TextPair> textPairs = dataset.getTextPairs();
        
        // Get annotations for both annotators
        List<Annotation> annotator1Annotations = annotationRepository.findByAnnotatorIdAndTextPair_DatasetId(
                annotator1Id, datasetId);
        List<Annotation> annotator2Annotations = annotationRepository.findByAnnotatorIdAndTextPair_DatasetId(
                annotator2Id, datasetId);

        // Create maps for quick lookup
        Map<Long, Long> annotator1Map = annotator1Annotations.stream()
                .collect(Collectors.toMap(a -> a.getTextPair().getId(), a -> a.getCategory().getId()));
        Map<Long, Long> annotator2Map = annotator2Annotations.stream()
                .collect(Collectors.toMap(a -> a.getTextPair().getId(), a -> a.getCategory().getId()));

        // Calculate observed agreement (Po)
        int agreements = 0;
        int totalComparisons = 0;

        for (TextPair textPair : textPairs) {
            Long category1 = annotator1Map.get(textPair.getId());
            Long category2 = annotator2Map.get(textPair.getId());

            if (category1 != null && category2 != null) {
                totalComparisons++;
                if (category1.equals(category2)) {
                    agreements++;
                }
            }
        }

        if (totalComparisons == 0) {
            return 0.0;
        }

        double observedAgreement = (double) agreements / totalComparisons;

        // Calculate expected agreement (Pe)
        Map<Long, Integer> categoryCounts = new HashMap<>();
        for (Long categoryId : annotator1Map.values()) {
            categoryCounts.merge(categoryId, 1, Integer::sum);
        }
        for (Long categoryId : annotator2Map.values()) {
            categoryCounts.merge(categoryId, 1, Integer::sum);
        }

        double expectedAgreement = 0.0;
        for (int count : categoryCounts.values()) {
            double probability = (double) count / (2 * totalComparisons);
            expectedAgreement += probability * probability;
        }

        // Calculate Cohen's Kappa
        if (expectedAgreement == 1.0) {
            return 1.0;
        }

        return (observedAgreement - expectedAgreement) / (1.0 - expectedAgreement);
    }

    /**
     * Calculate Fleiss' Kappa for a dataset.
     * Fleiss' Kappa is used when there are more than two annotators.
     *
     * @param datasetId the dataset ID
     * @return the Fleiss' Kappa score
     */
    public double calculateFleissKappa(Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));

        // Get all annotations for the dataset
        List<Annotation> allAnnotations = annotationRepository.findByTextPair_DatasetId(datasetId);

        // Group annotations by text pair
        Map<Long, List<Annotation>> annotationsByTextPair = allAnnotations.stream()
                .collect(Collectors.groupingBy(a -> a.getTextPair().getId()));

        // Get all unique categories
        Set<Long> allCategories = allAnnotations.stream()
                .map(a -> a.getCategory().getId())
                .collect(Collectors.toSet());

        int n = annotationsByTextPair.size(); // number of items
        int k = allCategories.size(); // number of categories
        int N = allAnnotations.size(); // total number of annotations

        if (n == 0 || k == 0 || N == 0) {
            return 0.0;
        }

        // Calculate Pj (proportion of assignments to category j)
        double[] Pj = new double[k];
        int categoryIndex = 0;
        for (Long categoryId : allCategories) {
            long categoryCount = allAnnotations.stream()
                    .filter(a -> a.getCategory().getId().equals(categoryId))
                    .count();
            Pj[categoryIndex++] = (double) categoryCount / N;
        }

        // Calculate Pi (proportion of agreement for item i)
        double[] Pi = new double[n];
        int itemIndex = 0;
        for (List<Annotation> itemAnnotations : annotationsByTextPair.values()) {
            int ni = itemAnnotations.size(); // number of annotators for this item
            if (ni == 0) {
                Pi[itemIndex++] = 0;
                continue;
            }

            double sum = 0;
            for (Long categoryId : allCategories) {
                long nij = itemAnnotations.stream()
                        .filter(a -> a.getCategory().getId().equals(categoryId))
                        .count();
                sum += nij * nij;
            }
            Pi[itemIndex++] = (sum - ni) / (ni * (ni - 1));
        }

        // Calculate P (mean of Pi)
        double P = Arrays.stream(Pi).sum() / n;

        // Calculate Pe (expected agreement by chance)
        double Pe = Arrays.stream(Pj).map(p -> p * p).sum();

        // Calculate Fleiss' Kappa
        if (Pe == 1.0) {
            return 1.0;
        }

        return (P - Pe) / (1.0 - Pe);
    }

    /**
     * Get agreement statistics for a dataset.
     *
     * @param datasetId the dataset ID
     * @return a map containing various agreement metrics
     */
    public Map<String, Object> getAgreementStatistics(Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));

        Map<String, Object> stats = new HashMap<>();
        
        // Calculate Fleiss' Kappa for the entire dataset
        double fleissKappa = calculateFleissKappa(datasetId);
        stats.put("fleissKappa", fleissKappa);
        stats.put("fleissKappaInterpretation", interpretKappa(fleissKappa));

        // Calculate Cohen's Kappa for each pair of annotators
        List<Map<String, Object>> pairwiseAgreements = new ArrayList<>();
        List<Long> annotatorIds = dataset.getAssignedAnnotators().stream()
                .map(User::getId)
                .toList();

        for (int i = 0; i < annotatorIds.size(); i++) {
            for (int j = i + 1; j < annotatorIds.size(); j++) {
                double cohensKappa = calculateCohensKappa(datasetId, annotatorIds.get(i), annotatorIds.get(j));
                Map<String, Object> pairStats = new HashMap<>();
                pairStats.put("annotator1Id", annotatorIds.get(i));
                pairStats.put("annotator2Id", annotatorIds.get(j));
                pairStats.put("cohensKappa", cohensKappa);
                pairStats.put("interpretation", interpretKappa(cohensKappa));
                pairwiseAgreements.add(pairStats);
            }
        }
        stats.put("pairwiseAgreements", pairwiseAgreements);

        return stats;
    }

    /**
     * Interpret a Kappa score according to Landis & Koch (1977).
     *
     * @param kappa the Kappa score
     * @return the interpretation string
     */
    private String interpretKappa(double kappa) {
        if (kappa < 0) return "Poor agreement";
        if (kappa < 0.20) return "Slight agreement";
        if (kappa < 0.40) return "Fair agreement";
        if (kappa < 0.60) return "Moderate agreement";
        if (kappa < 0.80) return "Substantial agreement";
        return "Almost perfect agreement";
    }
} 