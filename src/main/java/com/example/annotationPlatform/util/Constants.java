package com.example.annotationPlatform.util;

/**
 * Constants used throughout the application.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Role constants
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_ANNOTATOR = "ROLE_ANNOTATOR";

    // Task status constants
    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";

    // Annotation status constants
    public static final String ANNOTATION_STATUS_ANNOTATED = "ANNOTATED";

    // Dataset status constants
    public static final String DATASET_STATUS_CREATED = "CREATED";
    public static final String DATASET_STATUS_ACTIVE = "ACTIVE";
    public static final String DATASET_STATUS_COMPLETED = "COMPLETED";
    public static final String DATASET_STATUS_INACTIVE = "INACTIVE";
}