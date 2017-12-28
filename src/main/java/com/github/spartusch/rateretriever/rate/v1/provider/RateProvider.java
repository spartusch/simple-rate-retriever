package com.github.spartusch.rateretriever.rate.v1.provider;

import io.reactivex.Maybe;

import java.math.BigDecimal;

public interface RateProvider {

    Maybe<BigDecimal> getCurrentRate(final String symbol, final String currencyCode);

    boolean isCurrencyCodeSupported(final String currencyCode);

}
