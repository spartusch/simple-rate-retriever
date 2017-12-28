package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider;
import com.github.spartusch.rateretriever.rate.v1.provider.RateProviderType;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class RateServiceImpl implements RateService {

    private static final Logger log = LoggerFactory.getLogger(RateServiceImpl.class);
    private static final long defaultTimeoutSeconds = 30;

    private RateProvider stockExchangeRateProvider;
    private RateProvider coinMarketRateProvider;

    @Autowired
    public RateServiceImpl(@Autowired @Qualifier(RateProviderType.STOCK_EXCHANGE) final RateProvider stockExchangeRateProvider,
                           @Autowired @Qualifier(RateProviderType.COIN_MARKET) final RateProvider coinMarketRateProvider) {
        this.stockExchangeRateProvider = stockExchangeRateProvider;
        this.coinMarketRateProvider = coinMarketRateProvider;
    }

    private Maybe<String> getCurrentRate(final RateProvider provider, final String symbol, final String currencyCode,
                                         final String localeLanguage) {
        if (!provider.isCurrencyCodeSupported(currencyCode)) {
            return Maybe.error(new IllegalArgumentException("Currency code '" + currencyCode + "' not supported"));
        }

        final Locale locale = Locale.forLanguageTag(localeLanguage);

        return provider.getCurrentRate(symbol, currencyCode)
                .map(rate -> {
                    final NumberFormat numberFormat = DecimalFormat.getInstance(locale);
                    numberFormat.setMinimumFractionDigits(4);
                    return numberFormat.format(rate);
                });
    }

    @Override
    @Cacheable("CoinMarketRateCache")
    public String getCoinMarketRate(final String symbol, final String currencyCode, final String locale) {
        return getCurrentRate(coinMarketRateProvider, symbol, currencyCode, locale)
                .timeout(defaultTimeoutSeconds, TimeUnit.SECONDS)
                .doOnError(e -> log.error("Error getting digital currency rate", e))
                .blockingGet();
    }

    @Override
    @Cacheable("StockExchangeRateCache")
    public String getStockExchangeRate(final String symbol, final String currencyCode, final String locale) {
        return getCurrentRate(stockExchangeRateProvider, symbol, currencyCode, locale)
                .timeout(defaultTimeoutSeconds, TimeUnit.SECONDS)
                .doOnError(e -> log.error("Error getting stock exchange rate", e))
                .blockingGet();
    }

}
