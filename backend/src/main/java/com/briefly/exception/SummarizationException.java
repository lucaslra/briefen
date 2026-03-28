package com.briefly.exception;

public class SummarizationException extends RuntimeException {

    private final boolean timeout;

    public SummarizationException(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public SummarizationException(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
