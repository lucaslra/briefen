package com.briefen.exception;

public class SummaryNotFoundException extends RuntimeException {

    public SummaryNotFoundException(String id) {
        super("Summary not found: " + id);
    }
}
