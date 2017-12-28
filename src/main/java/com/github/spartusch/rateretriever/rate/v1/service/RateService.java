package com.github.spartusch.rateretriever.rate.v1.service;

public interface RateService {

    String getCoinMarketRate(final String symbol, final String currencyCode, final String locale);

    String getStockExchangeRate(final String symbol, final String currencyCode, final String locale);

}
