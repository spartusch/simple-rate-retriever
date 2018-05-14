package com.github.spartusch.rateretriever.rate.v1.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class RequestLoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final String suppressedUserAgent;

    public RequestLoggingFilter(@Value("${requests.logging.suppressUserAgent:}") final String suppressedUserAgent) {
        Objects.requireNonNull(suppressedUserAgent);
        log.info("Suppressing logs of requests by user agent: {}", suppressedUserAgent);
        this.suppressedUserAgent = suppressedUserAgent;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        final var headers = exchange.getRequest().getHeaders();
        final var userAgent = headers.getFirst("User-Agent");
        if (userAgent.isEmpty() || !userAgent.startsWith(suppressedUserAgent)) {
            log.info("Request: {}", exchange.getRequest().getURI());
            log.debug("Headers: {}", headers);
        }
        return chain.filter(exchange);
    }
}
