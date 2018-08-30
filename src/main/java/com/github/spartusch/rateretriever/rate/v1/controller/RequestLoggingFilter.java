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

    private final boolean enabled;
    private final String excludePath;

    public RequestLoggingFilter(
            @Value("${requestloggingfilter.enabled:true}") final boolean enabled,
            @Value("${requestloggingfilter.exclude.path:}") final String excludePath) {
        Objects.requireNonNull(excludePath);
        log.info("Request logging is enabled: {}", enabled);
        if (enabled) {
            log.info("Request path excluded from request logging: {}", excludePath);
        }
        this.enabled = enabled;
        this.excludePath = excludePath;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        if (enabled) {
            final var path = exchange.getRequest().getPath().pathWithinApplication().value();
            if (!path.startsWith(excludePath)) {
                log.info("Request: {}", exchange.getRequest().getURI());
                log.debug("Headers: {}", exchange.getRequest().getHeaders());
            }
        }
        return chain.filter(exchange);
    }
}
