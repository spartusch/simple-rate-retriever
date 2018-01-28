package com.github.spartusch.rateretriever.rate.v1.controller;

import com.github.spartusch.rateretriever.rate.v1.service.RateService;
import com.github.spartusch.webquery.WebQuery;
import com.github.spartusch.webquery.WebQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

@RestController
@RequestMapping("/rate/v1")
public class RateController {

    private static final String DEFAULT_LOCALE = "en-US";

    private static final Logger log = LoggerFactory.getLogger(RateController.class);

    private final RateService rateService;
    private final WebQueryService webQueryService;
    private final Duration eventInterval;

    @Autowired
    public RateController(final RateService rateService,
                          final WebQueryService webQueryService,
                          @Value("${rate.events.interval:10000}") final int eventIntervalMillis) {
        this.rateService = rateService;
        this.webQueryService = webQueryService;
        this.eventInterval = Duration.ofMillis(eventIntervalMillis);
    }

    private <T> Consumer<T> logResponse() {
        return r -> log.info("Response: '{}'", r);
    }

    @GetMapping(value = "/coinmarket/{symbol}/{currency}")
    public Mono<String> getCoinMarketRate(@PathVariable("symbol") final String symbol,
                                          @PathVariable("currency") final String currencyCode,
                                          @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return rateService.getCoinMarketRate(symbol, currencyCode, locale)
                .doOnNext(logResponse());
    }

    @GetMapping(value = "/stockexchange/{symbol}/{currency}")
    public Mono<String> getStockExchangeRate(@PathVariable("symbol") final String symbol,
                                             @PathVariable("currency") final String currencyCode,
                                             @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return rateService.getStockExchangeRate(symbol, currencyCode, locale)
                .doOnNext(logResponse());
    }

    @GetMapping(value = "/coinmarket/{symbol}/{currency}", produces = "text/event-stream")
    public Flux<String> getCoinMarketRatesStream(
            @PathVariable("symbol") final String symbol,
            @PathVariable("currency") final String currencyCode,
            @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return Flux.interval(Duration.ZERO, eventInterval)
                .flatMap(i -> rateService.getCoinMarketRate(symbol, currencyCode, locale))
                .distinct()
                .doOnNext(logResponse());
    }

    @GetMapping(value = "/stockexchange/{symbol}/{currency}", produces = "text/event-stream")
    public Flux<String> getStockExchangeRatesStream(
            @PathVariable("symbol") final String symbol,
            @PathVariable("currency") final String currencyCode,
            @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return Flux.interval(Duration.ZERO, eventInterval)
                .flatMap(i -> rateService.getStockExchangeRate(symbol, currencyCode, locale))
                .distinct()
                .doOnNext(logResponse());
    }

    @GetMapping(value = "/{provider}/{symbol}/{currency}/iqy")
    public HttpEntity<byte[]> downloadIqyFileForRequest(@PathVariable("provider") final String provider,
                                                        @PathVariable("symbol") final String symbol,
                                                        @PathVariable("currency") final String currencyCode,
                                                        final ServerHttpRequest request) {
        final WebQuery webQuery = webQueryService.createWebQuery(request.getURI().toString(), "/iqy");
        webQuery.setFileName(String.format("%s_%s.iqy", symbol, currencyCode.toUpperCase()));
        return webQuery.toHttpEntity();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleException(final IllegalArgumentException e) {
        log.error("Bad Request: {}", e);
        return e.getMessage();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(final RuntimeException e) {
        log.error("Internal Server Error: {}", e);
        return e.getMessage();
    }
}
