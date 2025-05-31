package com.example.annotationPlatform.service;

import com.example.annotationPlatform.exception.InvalidDatasetFormatException;
import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.model.*;
import com.example.annotationPlatform.repository.*;
import com.example.annotationPlatform.util.Constants;
import com.example.annotationPlatform.util.FileProcessingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing dataset-related operations, including creation, assignment, and metrics calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final TextPairRepository textPairRepository;
    private final CategoryRepository categoryRepository;
    private final AnnotationRepository annotationRepository;
    private final AnnotationTaskRepository annotationTaskRepository;
    private final AnnotationTaskService annotationTaskService;

    @Value("${application.file.upload.dir}")
    private String uploadDir;

    // --- READ Operations ---

    /**
     * Retrieves all datasets.
     *
     * @return list of datasets
     */
    public List<Dataset> getAllDatasets() {
        return datasetRepository.findAll();
    }

    /**
     * Retrieves a dataset by its ID.
     *
     * @param id the dataset ID
     * @return the dataset
     * @throws RessourceNotFoundException if dataset not found
     */
    public Dataset getDatasetById(Long id) {
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Dataset not found with ID: " + id));
        calculateCompletionRate(dataset);  // Calculate completion rate when retrieving dataset
        return dataset;
    }

    /**
     * Retrieves datasets by their status.
     *
     * @param status the status to filter by
     * @return list of datasets with the specified status
     */
    public List<Dataset> getDatasetsByStatus(String status) {
        return datasetRepository.findByStatus(status);
    }

    /**
     * Retrieves the source file of a dataset.
     *
     * @param id the dataset ID
     * @return byte array of the file content
     * @throws RuntimeException if file not found or error reading
     */
    public byte[] getDatasetSourceFile(Long id) {
        Dataset dataset = getDatasetById(id);
        String filePath = dataset.getSourceFilePath();

        if (filePath == null || filePath.isEmpty()) {
            throw new RuntimeException("No source file available for this dataset");
        }

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new RuntimeException("Source file no longer exists");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Error reading source file: " + e.getMessage(), e);
        }
    }

    // --- CREATE Operations ---

    /**
     * Creates a new dataset from a file and assigns categories.
     *
     * @param dataset the dataset details
     * @param file    the dataset file (CSV or JSON)
     * @param classes semicolon-separated category names
     * @param k       number of annotators per text pair
     * @throws InvalidDatasetFormatException if a file or categories are invalid
     */
    @Transactional
    public void createDataset(Dataset dataset, MultipartFile file, String classes, Integer k) {
        log.info("Starting creation of dataset: {}", dataset.getName());

        if (file == null || file.isEmpty()) {
            throw new InvalidDatasetFormatException("Dataset file is required.");
        }

        try {
            initializeDataset(dataset);
            dataset.setAnnotatorsPerTextPair(k);

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".json"))) {
                throw new InvalidDatasetFormatException("Unsupported file format. Use CSV or JSON.");
            }

            String filePath = FileProcessingUtil.saveFile(file, uploadDir);
            dataset.setSourceFilePath(filePath);

            List<Category> categories = processCategories(classes, dataset);
            if (categories.isEmpty()) {
                throw new InvalidDatasetFormatException("No valid categories provided");
            }

            List<TextPair> textPairs = FileProcessingUtil.processDataFromFile(file, dataset);
            if (textPairs.isEmpty()) {
                throw new InvalidDatasetFormatException("No valid text pairs found in the file");
            }

            saveDatasetWithRelations(dataset, categories, textPairs);

            log.info("Dataset '{}' created successfully with {} text pairs", dataset.getName(), textPairs.size());
        } catch (Exception e) {
            log.error("Error creating dataset: {}", e.getMessage(), e);
            throw new InvalidDatasetFormatException("Failed to create dataset: " + e.getMessage());
        }
    }

    /**
     * Initializes dataset with default values.
     *
     * @param dataset the dataset to initialize
     */
    private void initializeDataset(Dataset dataset) {
        if (dataset.getSourceFilePath() == null || dataset.getSourceFilePath().isEmpty()) {
            dataset.setSourceFilePath("temp-path");
        }
        dataset.setCreatedAt(LocalDateTime.now());
        dataset.setUpdatedAt(LocalDateTime.now());
        dataset.setStatus(Constants.DATASET_STATUS_CREATED);
    }

    /**
     * Processes categories from a semicolon-separated string.
     *
     * @param classes the category names
     * @param dataset the associated dataset
     * @return list of categories
     */
    private List<Category> processCategories(String classes, Dataset dataset) {
        List<Category> categoryList = new ArrayList<>();
        String[] classArray = classes.split(";");
        for (String className : classArray) {
            if (!className.trim().isEmpty()) {
                Category category = new Category();
                category.setName(className.trim());
                category.setDataset(dataset);
                categoryList.add(category);
            }
        }
        return categoryList;
    }

    /**
     * Saves dataset along with its categories and text pairs.
     *
     * @param dataset    the dataset
     * @param categories the categories
     * @param textPairs  the text pairs
     */
    private void saveDatasetWithRelations(Dataset dataset, List<Category> categories, List<TextPair> textPairs) {
        datasetRepository.save(dataset);
        categoryRepository.saveAll(categories);
        textPairRepository.saveAll(textPairs);
    }

    // --- UPDATE Operations ---

    /**
     * Updates dataset details.
     *
     * @param id             the dataset ID
     * @param datasetDetails the updated dataset details
     * @throws RessourceNotFoundException if dataset not found
     */
    @Transactional
    public void updateDataset(Long id, Dataset datasetDetails) {
        Dataset dataset = getDatasetById(id);
        dataset.setName(datasetDetails.getName());
        dataset.setDescription(datasetDetails.getDescription());
        dataset.setUpdatedAt(LocalDateTime.now());
        datasetRepository.save(dataset);
    }

    /**
     * Updates the status of a dataset.
     *
     * @param id     the dataset ID
     * @param status the new status
     * @throws RessourceNotFoundException if dataset not found
     */
    @Transactional
    public void updateDatasetStatus(Long id, String status) {
        Dataset dataset = getDatasetById(id);
        dataset.setStatus(status);
        dataset.setUpdatedAt(LocalDateTime.now());
        datasetRepository.save(dataset);
    }

    /**
     * Assigns annotators to a dataset and creates annotation tasks.
     *
     * @param datasetId    the dataset ID
     * @param annotatorIds list of annotator IDs
     * @throws RessourceNotFoundException if dataset or annotators not found
     * @throws IllegalArgumentException   if insufficient annotators
     */
    @Transactional
    public void assignAnnotators(Long datasetId, List<Long> annotatorIds) {
        log.info("Assigning {} annotators to dataset {}", annotatorIds.size(), datasetId);

        Dataset dataset = getDatasetById(datasetId);
        if (annotatorIds.isEmpty()) {
            log.warn("No annotators selected for assignment");
            throw new IllegalArgumentException("At least one annotator must be selected");
        }

        List<User> annotators = userRepository.findAllById(annotatorIds);
        if (annotators.size() != annotatorIds.size()) {
            log.error("Some users not found. IDs searched: {}", annotatorIds);
            throw new RessourceNotFoundException("One or more users not found");
        }

        List<User> validAnnotators = annotators.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(Constants.ROLE_ANNOTATOR)))
                .collect(Collectors.toList());

        if (validAnnotators.isEmpty()) {
            log.warn("No users have the annotator role");
            throw new IllegalArgumentException("No selected users have the annotator role");
        }

        annotationTaskService.createAnnotationTasksForDataset(dataset, validAnnotators);
        dataset.setAssignedAnnotators(validAnnotators);
        dataset.setStatus(Constants.DATASET_STATUS_ACTIVE);
        dataset.setUpdatedAt(LocalDateTime.now());
        datasetRepository.save(dataset);

        log.info("Assignment completed for {} annotators to dataset {}", validAnnotators.size(), datasetId);
    }

    /**
     * Unassigns an annotator from a dataset, keeping completed annotations.
     *
     * @param datasetId the dataset ID
     * @param userId    the annotator ID
     * @throws RessourceNotFoundException if dataset or user isn't found
     * @throws IllegalStateException      if annotator isn't assigned
     */
    @Transactional
    public void unassignAnnotator(Long datasetId, Long userId) {
        log.info("Unassigning annotator {} from dataset {}", userId, datasetId);

        Dataset dataset = getDatasetById(datasetId);
        User annotator = userRepository.findById(userId)
                .orElseThrow(() -> new RessourceNotFoundException("User not found with ID: " + userId));

        if (!dataset.getAssignedAnnotators().contains(annotator)) {
            log.warn("Annotator {} not assigned to dataset {}", userId, datasetId);
            throw new IllegalStateException("This annotator is not assigned to this dataset");
        }

        List<AnnotationTask> tasksToRemove = annotationTaskRepository.findByDatasetIdAndAnnotatorId(datasetId, userId)
                .stream()
                .filter(task -> !task.getStatus().equals(Constants.TASK_STATUS_COMPLETED))
                .collect(Collectors.toList());
        log.info("Removing {} non-completed tasks for annotator {}", tasksToRemove.size(), userId);
        annotationTaskRepository.deleteAll(tasksToRemove);

        dataset.getAssignedAnnotators().removeIf(user -> user.getId().equals(userId));
        if (dataset.getAssignedAnnotators().isEmpty()) {
            dataset.setStatus(Constants.DATASET_STATUS_CREATED);
            log.info("All annotators unassigned, dataset set to CREATED");
        }

        dataset.setUpdatedAt(LocalDateTime.now());
        datasetRepository.save(dataset);

        log.info("Unassignment of annotator {} completed successfully", userId);
    }

    // --- DELETE Operation ---

    /**
     * Deletes a dataset and its associated data.
     *
     * @param id the dataset ID
     * @throws RessourceNotFoundException if dataset not jammer
     * */
     @Transactional
     public void deleteDataset(Long id) {
         Dataset dataset = getDatasetById(id);

         String filePath = dataset.getSourceFilePath();
         if (filePath != null && !filePath.isEmpty()) {
             try {
                Path path = Paths.get(filePath);
                 if (Files.exists(path)) {
                    Files.delete(path);
                 }
             } catch (IOException e) {
                log.error("Error deleting file: {}", e.getMessage(), e);
             }
         }

         List<TextPair> textPairs = dataset.getTextPairs();
         for (TextPair pair : textPairs) {
         annotationRepository.deleteAll(pair.getAnnotations());
         }

         categoryRepository.deleteAll(dataset.getCategories());
         textPairRepository.deleteAll(textPairs);
         datasetRepository.delete(dataset);
     }

     // --- Analysis Operations ---

     /**
      * Calculates metrics for a dataset, including annotations by category and agreement rate.
      *
      * @param id the dataset ID
     * @return map of metrics
     * @throws RessourceNotFoundException if dataset not found
     */
    public Map<String, Object> calculateMetrics(Long id) {
        Dataset dataset = getDatasetById(id);
        List<TextPair> textPairs = dataset.getTextPairs();
        int K = dataset.getAnnotatorsPerTextPair() != null ? dataset.getAnnotatorsPerTextPair() : 1;

        Map<String, Object> metrics = new HashMap<>();
        Map<String, Integer> annotationsByCategory = new HashMap<>();
        double agreementRate = 0.0;
        int totalAgreements = 0;
        int totalPairsWithEnoughAnnotations = 0;
        double averageAnnotationTime = 0.0;

        for (TextPair pair : textPairs) {
            Set<Annotation> annotationsSet = pair.getAnnotations();
            List<Annotation> annotations = new ArrayList<>(annotationsSet);

            for (Annotation annotation : annotations) {
                String categoryName = annotation.getCategory().getName();
                annotationsByCategory.put(categoryName,
                        annotationsByCategory.getOrDefault(categoryName, 0) + 1);
            }

            if (!annotations.isEmpty()) {
                long totalDuration = annotations.stream()
                        .filter(a -> a.getAnnotationDurationMs() != null)
                        .mapToLong(Annotation::getAnnotationDurationMs)
                        .sum();
                averageAnnotationTime += totalDuration / (double) annotations.size();
            }

            if (annotations.size() >= K) {
                totalPairsWithEnoughAnnotations++;
                String firstCategory = annotations.getFirst().getCategory().getName();
                boolean allAgree = true;

                for (int i = 1; i < annotations.size(); i++) {
                    if (!annotations.get(i).getCategory().getName().equals(firstCategory)) {
                        allAgree = false;
                        break;
                    }
                }

                if (allAgree) {
                    totalAgreements++;
                }
            }
        }

        if (totalPairsWithEnoughAnnotations > 0) {
            agreementRate = (double) totalAgreements / totalPairsWithEnoughAnnotations;
        }

        if (!textPairs.isEmpty()) {
            averageAnnotationTime = averageAnnotationTime / textPairs.size();
        }

        metrics.put("annotationsByCategory", annotationsByCategory);
        metrics.put("agreementRate", agreementRate);
        metrics.put("totalAnnotations", textPairs.stream()
                .mapToInt(pair -> pair.getAnnotations().size())
                .sum());
        metrics.put("completionRate", calculateCompletionRate(dataset));
        metrics.put("averageAnnotationTime", averageAnnotationTime);

        if (calculateCompletionRate(dataset) >= 1.0) {
            dataset.setStatus(Constants.DATASET_STATUS_COMPLETED);
            dataset.setUpdatedAt(LocalDateTime.now());
            datasetRepository.save(dataset);
        }

        return metrics;
    }

    /**
     * Calculates the completion rate of a dataset.
     *
     * @param dataset the dataset
     * @return the completion rate (0.0 to 1.0)
     */
    public double calculateCompletionRate(Dataset dataset) {
        int totalPairs = dataset.getTextPairs().size();
        int K = dataset.getAnnotatorsPerTextPair() != null ? dataset.getAnnotatorsPerTextPair() : 1;

        if (totalPairs == 0) {
            dataset.setCompletionRate(0.0);
            return 0.0;
        }

        int expectedAnnotations = totalPairs * K;
        int actualAnnotations = dataset.getTextPairs().stream()
                .mapToInt(pair -> pair.getAnnotations().size())
                .sum();
        
        double completionRate = (double) actualAnnotations / expectedAnnotations;
        dataset.setCompletionRate(completionRate);
        datasetRepository.save(dataset);  // Save the updated completion rate

        // Update status if completed
        if (completionRate >= 1.0 && !dataset.getStatus().equals(Constants.DATASET_STATUS_COMPLETED)) {
            dataset.setStatus(Constants.DATASET_STATUS_COMPLETED);
            dataset.setUpdatedAt(LocalDateTime.now());
            datasetRepository.save(dataset);
        }

        return completionRate;
    }

    /**
     * Retrieves general statistics for all datasets.
     *
     * @return map of statistics
     */
    public Map<String, Object> getDatasetStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        statistics.put("totalDatasets", datasetRepository.count());
        statistics.put("inactive", datasetRepository.countByStatus(Constants.DATASET_STATUS_INACTIVE));
        statistics.put("completed", datasetRepository.countByStatus(Constants.DATASET_STATUS_COMPLETED));
        statistics.put("created", datasetRepository.countByStatus(Constants.DATASET_STATUS_CREATED));
        statistics.put("active", datasetRepository.countByStatus(Constants.DATASET_STATUS_ACTIVE));
        statistics.put("totalAnnotations", annotationRepository.count());
        statistics.put("totalUsers", userRepository.findByRoles_Name(Constants.ROLE_ANNOTATOR).size());

        return statistics;
    }

    /**
     * Exports a dataset to CSV format.
     *
     * @param id the dataset ID
     * @return byte array of CSV content
     * @throws RessourceNotFoundException if dataset not found
     */
    public byte[] exportDatasetToCsv(Long id) {
        Dataset dataset = getDatasetById(id);
        List<TextPair> textPairs = dataset.getTextPairs();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("ID,Text1,Text2");
            for (TextPair pair : textPairs) {
                writer.printf("%d,\"%s\",\"%s\"%n",
                        pair.getId(),
                        pair.getText1().replace("\"", "\"\""),
                        pair.getText2().replace("\"", "\"\""),
                        pair.getText1().replace("\"", "\"\""),
                        pair.getText2().replace("\"", "\"\""));
            }
        }

        return out.toByteArray();
    }

    /**
     * Exports annotations of a dataset to CSV format.
     *
     * @param id the dataset ID
     * @return byte array of CSV content
     * @throws RessourceNotFoundException if dataset not found
     */
    public byte[] exportAnnotationsToCsv(Long id) {
        Dataset dataset = getDatasetById(id);
        List<TextPair> textPairs = dataset.getTextPairs();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("TextPairID,Text1,Text2,Annotator,Category,AnnotatedAt");
            for (TextPair pair : textPairs) {
                for (Annotation annotation : pair.getAnnotations()) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            pair.getId(),
                            pair.getText1().replace("\"", "\"\""),
                            pair.getText2().replace("\"", "\"\""),
                            annotation.getAnnotator().getUsername(),
                            annotation.getCategory().getName(),
                            annotation.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm")));
                }
            }
        }

        return out.toByteArray();
    }

    /**
     * Export annotation of datset to JSON format.
     * @param id the dataset ID
     * @return byte array of JSON content
     * @throws RessourceNotFoundException if dataset not found
     */
    public byte[] exportAnnotationsToJson(Long id) {
        Dataset dataset = getDatasetById(id);
        List<TextPair> textPairs = dataset.getTextPairs();
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> exportData = new ArrayList<>();
        for (TextPair pair : textPairs) {
            for (Annotation annotation : pair.getAnnotations()) {
                Map<String, Object> data = new HashMap<>();
                data.put("textPairId", pair.getId());
                data.put("text1", pair.getText1());
                data.put("text2", pair.getText2());
                data.put("annotator", annotation.getAnnotator().getUsername());
                data.put("category", annotation.getCategory().getName());
                data.put("annotatedAt", annotation.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm")));
                exportData.add(data);
            }
        }
        try {
            return mapper.writeValueAsString(exportData).getBytes();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error exporting to JSON", e);
        }
    }
}