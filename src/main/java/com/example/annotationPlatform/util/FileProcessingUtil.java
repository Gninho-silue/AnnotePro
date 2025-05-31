package com.example.annotationPlatform.util;

import com.example.annotationPlatform.exception.InvalidDatasetFormatException;
import com.example.annotationPlatform.model.Dataset;
import com.example.annotationPlatform.model.TextPair;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for processing dataset files (CSV and JSON).
 */
@Slf4j
public class FileProcessingUtil {

    /**
     * Processes a dataset file and extracts text pairs.
     *
     * @param file    the dataset file
     * @param dataset the associated dataset
     * @return list of text pairs
     * @throws IOException if file processing fails
     * @throws InvalidDatasetFormatException if a file format is invalid
     */
    public static List<TextPair> processDataFromFile(MultipartFile file, Dataset dataset) throws IOException {
        log.info("Processing file: {}", file.getOriginalFilename());

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        if (filename.endsWith(".csv")) {
            return processCSVFile(file, dataset);
        } else if (filename.endsWith(".json")) {
            return processJSONFile(file, dataset);
        } else {
            throw new InvalidDatasetFormatException("Unsupported file format. Use CSV or JSON");
        }
    }

    /**
     * Processes a CSV file to extract text pairs.
     *
     * @param file    the CSV file
     * @param dataset the associated dataset
     * @return list of text pairs
     * @throws IOException if file reading fails
     */
    private static List<TextPair> processCSVFile(MultipartFile file, Dataset dataset) throws IOException {
        log.info("Processing CSV file: {}", file.getOriginalFilename());
        List<TextPair> textPairs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (values.length >= 2) {
                    String text1 = values[0].trim().replace("\"", "");
                    String text2 = values[1].trim().replace("\"", "");

                    if (!text1.isEmpty() && !text2.isEmpty()) {
                        TextPair textPair = createTextPair(text1, text2, dataset);
                        textPairs.add(textPair);
                    }
                }
            }
        }

        log.info("CSV file processed: {} text pairs extracted", textPairs.size());
        return textPairs;
    }

    /**
     * Processes a JSON file to extract text pairs.
     *
     * @param file    the JSON file
     * @param dataset the associated dataset
     * @return list of text pairs
     * @throws IOException if file reading fails
     */
    private static List<TextPair> processJSONFile(MultipartFile file, Dataset dataset) throws IOException {
        log.info("Processing JSON file: {}", file.getOriginalFilename());
        List<TextPair> textPairs = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(is);

            if (rootNode.isArray()) {
                extractTextPair(dataset, textPairs, rootNode);
            } else {
                if (rootNode.has("pairs") && rootNode.get("pairs").isArray()) {
                    JsonNode pairsNode = rootNode.get("pairs");
                    extractTextPair(dataset, textPairs, pairsNode);
                } else {
                    throw new IllegalArgumentException("Invalid JSON format. Expected: [{\"text1\": \"...\", \"text2\": \"...\"}, {...}] or {\"pairs\": [{\"text1\": \"...\", \"text2\": \"...\"}, {...}]}");
                }
            }
        }

        log.info("JSON file processed: {} text pairs extracted", textPairs.size());
        return textPairs;
    }

    /**
     * Extracts text pairs from a JSON node.
     *
     * @param dataset    the associated dataset
     * @param textPairs  the list to add text pairs to
     * @param rootNode   the JSON node containing pairs
     */
    private static void extractTextPair(Dataset dataset, List<TextPair> textPairs, JsonNode rootNode) {
        for (JsonNode pairNode : rootNode) {
            String text1 = pairNode.has("text1") ? pairNode.get("text1").asText() : null;
            String text2 = pairNode.has("text2") ? pairNode.get("text2").asText() : null;

            if (text1 != null && !text1.isEmpty() && text2 != null && !text2.isEmpty()) {
                TextPair textPair = createTextPair(text1, text2, dataset);
                textPairs.add(textPair);
            }
        }
    }

    /**
     * Creates a new text pair.
     *
     * @param text1   the first text
     * @param text2   the second text
     * @param dataset the associated dataset
     * @return the created text pair
     */
    private static TextPair createTextPair(String text1, String text2, Dataset dataset) {
        TextPair textPair = new TextPair();
        textPair.setText1(text1);
        textPair.setText2(text2);
        textPair.setDataset(dataset);
        return textPair;
    }

    /**
     * Saves a file to the specified upload directory.
     *
     * @param file      the file to save
     * @param uploadDir the upload directory
     * @return the file path
     * @throws IOException if file saving fails
     */
    public static String saveFile(MultipartFile file, String uploadDir) throws IOException {
        log.info("Upload directory: {}", uploadDir);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            try {
                log.info("Creating directory: {}", uploadPath.toAbsolutePath());
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                log.warn("Error creating directory: {}", e.getMessage());
                throw new IOException("Failed to create upload directory: " + e.getMessage(), e);
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("No filename provided");
        }
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = System.currentTimeMillis() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        log.info("Saving file to: {}", filePath.toAbsolutePath());
        Files.copy(file.getInputStream(), filePath);
        return filePath.toString();
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param filename the filename
     * @return the file extension
     */
    private static String getFileExtension(String filename) {
        if (filename == null) return ".csv";
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : ".csv";
    }
}