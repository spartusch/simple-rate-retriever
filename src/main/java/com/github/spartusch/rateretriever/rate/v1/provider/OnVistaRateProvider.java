package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository(RateProviderType.STOCK_EXCHANGE)
public class OnVistaRateProvider extends AbstractRateProvider implements RateProvider {

    private static final Pattern assetPagePattern = Pattern.compile("\"snapshotlink\":\"([^\"]+)\"");
    private static final Pattern amountPattern = Pattern.compile(
            "<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|Umrechnung:</a>\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>"
    );

    private final String baseSearchUrl;
    private final Map<String, String> symbolToUrlCache;

    @Autowired
    public OnVistaRateProvider(@Value("${provider.onVista.url}") final String url) {
        super(RateProviderType.STOCK_EXCHANGE);
        this.baseSearchUrl = url;
        this.symbolToUrlCache = new HashMap<>();
    }

    private Mono<String> extractPattern(final Pattern pattern, final String content, final String errorMessage) {
        return Mono.fromCallable(() -> {
            final var matcher = pattern.matcher(content);
            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return matcher.group(i);
                    }
                }
            }
            throw new DataExtractionException(errorMessage);
        });
    }

    @Override
    public Mono<BigDecimal> getCurrentRate(final String symbol, final String currencyCode) {
        return Mono.fromCallable(() -> symbolToUrlCache.get(symbol))
                .switchIfEmpty(
                        Mono.just(baseSearchUrl + symbol)
                                // Search for the asset link
                                .flatMap(searchUrl -> getUrl(searchUrl, MediaType.APPLICATION_JSON_VALUE))
                                .flatMap(searchResult -> extractPattern(assetPagePattern, searchResult, "Asset not found"))
                                .doOnSuccess(url -> symbolToUrlCache.put(symbol, url))
                )
                // Retrieve the asset page
                .flatMap(url -> getUrl(url, MediaType.TEXT_HTML_VALUE))
                .doOnError(e -> symbolToUrlCache.remove(symbol))
                .retry(1)
                // Extract the amount
                .flatMap(assetPage -> extractPattern(amountPattern, assetPage, "Amount not found"))
                .flatMap(amount -> toBigDecimal(Locale.GERMANY, amount));
    }

    @Override
    public Mono<Boolean> isCurrencyCodeSupported(final String currencyCode) {
        return Mono.just("EUR".equalsIgnoreCase(currencyCode));
    }
}
