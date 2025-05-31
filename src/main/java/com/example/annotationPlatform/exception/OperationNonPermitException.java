package com.example.annotationPlatform.exception;

public class OperationNonPermitException extends RuntimeException {
    public OperationNonPermitException(String message) {
        super(message);
    }
}
