package com.github.spartusch.rateretriever.rate.v1.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private static final Logger log = LoggerFactory.getLogger(AbstractRateProvider.class);

    protected Mono<String> getUrl(final String url, final String accept) {
        return WebClient.create(url)
                .get()
                .accept(MediaType.parseMediaType(accept))
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSubscribe(s -> log.info("Fetching {}", url));
    }

    protected Mono<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        return Mono.fromCallable(() -> {
            final NumberFormat numberFormat = NumberFormat.getInstance(locale);
            return new BigDecimal(numberFormat.parse(amount).toString());
        });
    }
}
