package com.briefly.exception;

public class ArticleFetchException extends RuntimeException {

    public ArticleFetchException(String message) {
        super(message);
    }

    public ArticleFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
