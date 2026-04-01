package com.briefen.config;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class MongoConnectionFailureAnalyzer extends AbstractFailureAnalyzer<UnsatisfiedDependencyException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnsatisfiedDependencyException cause) {
        if (!isMongoTemplateBeanFailure(cause)) {
            return null;
        }

        String uri = System.getenv("BRIEFEN_MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            return new FailureAnalysis(
                    "BRIEFEN_MONGODB_URI is not set — the application cannot connect to MongoDB.",
                    "Set BRIEFEN_MONGODB_URI to your MongoDB connection string.\n" +
                    "  Example: BRIEFEN_MONGODB_URI=mongodb://localhost:27017/briefen\n" +
                    "  For local development, copy .env.example to .env and run make dev.",
                    cause
            );
        }

        return new FailureAnalysis(
                "MongoDB connection failed. BRIEFEN_MONGODB_URI is set to: " + redact(uri),
                "Check that MongoDB is running and reachable at the configured URI.\n" +
                "  Run: docker compose up -d",
                cause
        );
    }

    private boolean isMongoTemplateBeanFailure(UnsatisfiedDependencyException cause) {
        Throwable t = cause;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("mongoTemplate")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private String redact(String uri) {
        // Hide credentials: mongodb://user:pass@host → mongodb://****@host
        return uri.replaceAll("//[^@]+@", "//****@");
    }
}
