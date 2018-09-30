package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.spartusch.rateretriever.rate.v1.exception.WebRetrievalException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;

abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private final Timer requestTimer;
    private final HttpClient httpClient;

    AbstractRateProvider(final String providerName) {
        requestTimer = Metrics.timer("provider.requests", "provider.name", providerName);
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    Mono<String> getUrl(final String url, final String accept) {
        try {
            var request = HttpRequest.newBuilder(new URI(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", accept)
                    .build();
            return Mono.fromCallable(() -> requestTimer.recordCallable(() ->
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                    ))
                    .doOnNext(response -> {
                        if (response.statusCode() >= 400) {
                            throw new WebRetrievalException(url, "Status: " + response.statusCode());
                        }
                    })
                    .map(HttpResponse::body);
        } catch (final Exception e) {
            return Mono.error(new WebRetrievalException(url, e));
        }
    }

    Mono<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        return Mono.fromCallable(() -> {
            final var numberFormat = NumberFormat.getInstance(locale);
            return new BigDecimal(numberFormat.parse(amount).toString());
        });
    }
}
