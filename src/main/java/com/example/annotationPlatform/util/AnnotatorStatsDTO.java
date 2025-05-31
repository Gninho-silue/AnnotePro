package com.example.annotationPlatform.util;

import lombok.Getter;

import java.util.Map;


public record AnnotatorStatsDTO(
        Long userId,
        String username,
        String email,
        long totalTasks,
        long completedTasks,
        long totalAnnotations,
        long avgAnnotationTimeMs,
        Map<Long, DatasetProgress> datasetProgress
) {
    public record DatasetProgress(
            String datasetName,
            long totalTasks,
            long completedTasks
    ) {
        public double getCompletionRate() {
            return totalTasks == 0 ? 0.0 : (double) completedTasks / totalTasks;
        }
    }

    public double getCompletionRate() {
        if (datasetProgress == null || datasetProgress.isEmpty()) {
            return totalTasks == 0 ? 0.0 : (double) completedTasks / totalTasks;
        }
        
        double totalProgress = datasetProgress.values().stream()
                .mapToDouble(DatasetProgress::getCompletionRate)
                .sum();
        return totalProgress / datasetProgress.size();
    }

    public String getFormattedAvgAnnotationTime() {
        if (avgAnnotationTimeMs == 0) {
            return "N/A";
        }
        long seconds = avgAnnotationTimeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d min %d s", minutes, seconds);
    }

    // Getters pour les champs du record
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public long getTotalTasks() { return totalTasks; }
    public long getCompletedTasks() { return completedTasks; }
    public long getTotalAnnotations() { return totalAnnotations; }
    public long getAvgAnnotationTimeMs() { return avgAnnotationTimeMs; }
    public Map<Long, DatasetProgress> getDatasetProgress() { return datasetProgress; }
}
