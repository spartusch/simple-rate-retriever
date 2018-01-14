package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider;
import com.github.spartusch.rateretriever.rate.v1.provider.RateProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class RateServiceImpl implements RateService {

    private static final Logger log = LoggerFactory.getLogger(RateServiceImpl.class);

    private RateProvider stockExchangeRateProvider;
    private RateProvider coinMarketRateProvider;

    @Autowired
    public RateServiceImpl(@Autowired @Qualifier(RateProviderType.STOCK_EXCHANGE) final RateProvider stockExchangeRateProvider,
                           @Autowired @Qualifier(RateProviderType.COIN_MARKET) final RateProvider coinMarketRateProvider) {
        this.stockExchangeRateProvider = stockExchangeRateProvider;
        this.coinMarketRateProvider = coinMarketRateProvider;
    }

    // Visible for testing
    Mono<String> getCurrentRate(final RateProvider provider, final String symbol, final String currencyCode,
                                final String locale) {
        log.info("Request: '{}', '{}', '{}'", symbol, currencyCode, locale);
        return provider.isCurrencyCodeSupported(currencyCode)
                .map(isSupported -> {
                    if (!isSupported) {
                        throw new IllegalArgumentException("Currency code '" + currencyCode + "' not supported");
                    }
                    return true;
                })
                .then(provider.getCurrentRate(symbol, currencyCode))
                .map(rate -> {
                    final NumberFormat numberFormat = DecimalFormat.getInstance(Locale.forLanguageTag(locale));
                    numberFormat.setMinimumFractionDigits(4);
                    return numberFormat.format(rate);
                })
                .doOnError(e -> log.error("Error getting rate: {}, {}, {}", symbol, currencyCode, locale, e))
                .cache()
                .doOnNext(result -> log.info("Response: '{}', '{}', '{}' -> '{}'", symbol, currencyCode, locale, result));
    }

    @Override
    @Cacheable("CoinMarketRateCache")
    public Mono<String> getCoinMarketRate(final String symbol, final String currencyCode, final String locale) {
        return getCurrentRate(coinMarketRateProvider, symbol, currencyCode, locale);
    }

    @Override
    @Cacheable("StockExchangeRateCache")
    public Mono<String> getStockExchangeRate(final String symbol, final String currencyCode, final String locale) {
        return getCurrentRate(stockExchangeRateProvider, symbol, currencyCode, locale);
    }
}
