package com.github.spartusch.rateretriever.rate.v1.service;

import reactor.core.publisher.Mono;

public interface RateService {

    Mono<String> getCoinMarketRate(final String symbol, final String currencyCode, final String locale);

    Mono<String> getStockExchangeRate(final String symbol, final String currencyCode, final String locale);

}
