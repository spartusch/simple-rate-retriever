package com.github.spartusch.rateretriever.rate.v1.provider;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface RateProvider {

    Mono<BigDecimal> getCurrentRate(final String symbol, final String currencyCode);

    Mono<Boolean> isCurrencyCodeSupported(final String currencyCode);

}
