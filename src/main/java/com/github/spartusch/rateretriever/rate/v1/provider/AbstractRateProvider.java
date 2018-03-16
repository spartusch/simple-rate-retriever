package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.spartusch.rateretriever.rate.v1.exception.WebRetrievalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private static final Logger log = LoggerFactory.getLogger(AbstractRateProvider.class);

    private Mono<ClientResponse> getUrl(final String url, final String accept) {
        return WebClient.create(url)
                .get()
                .accept(MediaType.parseMediaType(accept))
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .exchange()
                .doOnNext(response -> log.info("Fetching {}", url));
    }

    protected <T> Mono<T> getUrl(final String url, final String accept, final Class<T> clazz) {
        return getUrl(url, accept)
                .flatMap(clientResponse -> {
                    // Follow redirects explicitly (cf. https://jira.spring.io/browse/SPR-16277)
                    final List<String> locations = clientResponse.headers().header("Location");
                    if (clientResponse.statusCode().is3xxRedirection() && !locations.isEmpty()) {
                        try {
                            final URI redirectUri = new URI(url).resolve(locations.get(0));
                            return getUrl(redirectUri.toString(), accept);
                        } catch (final URISyntaxException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                    return Mono.just(clientResponse);
                })
                .doOnNext(clientResponse -> {
                    if (clientResponse.statusCode().isError()) {
                        throw new WebRetrievalException(url, clientResponse.statusCode().getReasonPhrase());
                    }
                })
                .flatMap(clientResponse -> clientResponse.bodyToMono(clazz));
    }

    protected Mono<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        return Mono.fromCallable(() -> {
            final NumberFormat numberFormat = NumberFormat.getInstance(locale);
            return new BigDecimal(numberFormat.parse(amount).toString());
        });
    }
}
