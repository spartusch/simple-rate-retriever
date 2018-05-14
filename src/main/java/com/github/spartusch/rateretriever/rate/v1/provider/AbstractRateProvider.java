package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.spartusch.rateretriever.rate.v1.exception.WebRetrievalException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private final Timer requestTimer;

    protected AbstractRateProvider(final String providerName) {
        requestTimer = Metrics.timer("provider.requests", "provider.name", providerName);
    }

    protected Mono<String> getUrl(final String url, final String accept) {
        return Mono.fromCallable(requestTimer.wrap(() -> {
            try {
                return Request.Get(url).userAgent(USER_AGENT).setHeader(HttpHeaders.ACCEPT, accept).execute().returnResponse();
            } catch (IOException e) {
                throw new WebRetrievalException(url, e);
            }
        })).doOnNext(response -> {
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new WebRetrievalException(url, response.getStatusLine().toString());
            }
        }).map(response -> {
            try {
                final var entity = response.getEntity();
                return EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset());
            } catch (IOException e) {
                throw new WebRetrievalException(url, e);
            }
        });
    }

    protected Mono<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        return Mono.fromCallable(() -> {
            final var numberFormat = NumberFormat.getInstance(locale);
            return new BigDecimal(numberFormat.parse(amount).toString());
        });
    }
}
