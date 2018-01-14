package com.github.spartusch.rateretriever.rate.v1.provider;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository(RateProviderType.COIN_MARKET)
public class CoinMarketCapRateProvider extends AbstractRateProvider implements RateProvider {

    private static final String URL_PATH = "/v1/ticker/";

    private final List<String> supportedCurrencies = Collections.unmodifiableList(Arrays.asList(
            "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR",
            "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PKR", "PLN", "RUB", "SEK", "SGD", "THB", "TRY", "TWD",
            "ZAR", "USD"
    ));
    private final Pattern extractionPattern = Pattern.compile("\"price_([a-z]+)\": \"([0-9.,]+)\"", Pattern.CASE_INSENSITIVE);
    private final String baseUrl;

    @Autowired
    public CoinMarketCapRateProvider(@Value("${provider.coinMarketCap.baseUrl}") final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Mono<BigDecimal> getCurrentRate(final String symbol, final String currencyCode) {
        return Mono.fromCallable(() -> {
                    final URIBuilder uriBuilder = new URIBuilder(baseUrl);
                    uriBuilder.setPath(URL_PATH + symbol);
                    uriBuilder.setParameter("convert", currencyCode);
                    return uriBuilder.build().toString();
                })
                .flatMap(url -> getUrl(url, MediaType.APPLICATION_JSON_VALUE))
                .map(content -> {
                    final Matcher matcher = extractionPattern.matcher(content);
                    while (matcher.find()) {
                        final String currencyCodeMatch = matcher.group(1);
                        if (currencyCodeMatch.equalsIgnoreCase(currencyCode)) {
                            return matcher.group(2);
                        }
                    }
                    throw new RuntimeException("Amount not found");
                })
                .flatMap(amount -> toBigDecimal(Locale.US, amount));
    }

    @Override
    public Mono<Boolean> isCurrencyCodeSupported(final String currencyCode) {
        return Mono.just(currencyCode != null && supportedCurrencies.contains(currencyCode.toUpperCase()));
    }
}
