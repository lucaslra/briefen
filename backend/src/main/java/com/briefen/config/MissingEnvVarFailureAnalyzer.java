package com.briefen.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class MissingEnvVarFailureAnalyzer extends AbstractFailureAnalyzer<IllegalArgumentException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, IllegalArgumentException cause) {
        String msg = cause.getMessage();
        if (msg == null || !msg.contains("MONGODB_URI")) {
            return null;
        }
        return new FailureAnalysis(
                "Required environment variable MONGODB_URI is not set.",
                "Set MONGODB_URI to your MongoDB connection string.\n" +
                "  Example: MONGODB_URI=mongodb://localhost:27017/briefen\n" +
                "  For local development, copy .env.example to .env — the app will load it automatically.",
                cause
        );
    }
}
