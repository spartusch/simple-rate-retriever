package com.github.spartusch.rateretriever.rate.v1.exception;

public class DataExtractionException extends RuntimeException {

    public DataExtractionException(final String message) {
        super(message);
    }

    public DataExtractionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DataExtractionException(final Throwable cause) {
        super(cause);
    }
}
