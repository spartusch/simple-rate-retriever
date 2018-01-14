package com.github.spartusch.rateretriever.rate.v1.provider;

import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private static final Logger log = LoggerFactory.getLogger(AbstractRateProvider.class);

    protected Mono<String> getUrl(final String url, final String accept) {
        return Mono.fromCallable(() -> {
            log.info("Fetching {}", url);
            return Request.Get(url)
                    .addHeader("Accept", accept)
                    .addHeader("User-Agent", USER_AGENT)
                    .execute()
                    .returnContent()
                    .asString();
        });
    }

    protected Mono<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        return Mono.fromCallable(() -> {
            final NumberFormat numberFormat = NumberFormat.getInstance(locale);
            return new BigDecimal(numberFormat.parse(amount).toString());
        });
    }
}
