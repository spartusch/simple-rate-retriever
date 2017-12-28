package com.github.spartusch.rateretriever.rate.v1.provider;

import io.reactivex.Maybe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository(RateProviderType.STOCK_EXCHANGE)
public class OnVistaRateProvider extends AbstractRateProvider implements RateProvider {

    private static final String URL_PATH = "/api/header/search?q=";

    private static final Pattern assetPagePattern = Pattern.compile("\"snapshotlink\":\"([^\"]+)\"");
    private static final Pattern amountPattern = Pattern.compile(
            "<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|Umrechnung:</a>\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>"
    );

    private final String baseSearchUrl;
    private final Map<String, String> symbolToUrlCache;

    @Autowired
    public OnVistaRateProvider(@Value("${provider.onVista.baseUrl}") final String baseUrl) {
        this.baseSearchUrl = baseUrl + URL_PATH;
        this.symbolToUrlCache = new HashMap<>();
    }

    private Maybe<String> extractPattern(final Pattern pattern, final String content, final String errorMessage) {
        final Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return Maybe.just(matcher.group(i));
                }
            }
        }
        return Maybe.error(new RuntimeException(errorMessage));
    }

    @Override
    public Maybe<BigDecimal> getCurrentRate(final String symbol, final String currencyCode) {
        return Maybe.fromCallable(() -> symbolToUrlCache.get(symbol))
                .switchIfEmpty(
                        Maybe.just(baseSearchUrl + symbol)
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
    public boolean isCurrencyCodeSupported(final String currencyCode) {
        return "EUR".equalsIgnoreCase(currencyCode);
    }

}
