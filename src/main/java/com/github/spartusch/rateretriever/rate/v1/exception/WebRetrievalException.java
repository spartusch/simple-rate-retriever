package com.github.spartusch.rateretriever.rate.v1.exception;

public class WebRetrievalException extends RuntimeException {

    private final String requestUrl;

    public WebRetrievalException(final String requestUrl, final String message) {
        super(requestUrl + ": " + message);
        this.requestUrl = requestUrl;
    }

    public WebRetrievalException(final String requestUrl, final String message, final Throwable cause) {
        super(requestUrl + ": " + message, cause);
        this.requestUrl = requestUrl;
    }

    public WebRetrievalException(final String requestUrl, final Throwable cause) {
        super(requestUrl, cause);
        this.requestUrl = requestUrl;
    }

    public String getRequestUrl() {
        return requestUrl;
    }
}
