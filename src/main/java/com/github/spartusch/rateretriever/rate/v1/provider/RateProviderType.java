package com.github.spartusch.rateretriever.rate.v1.provider;

public class RateProviderType {

    public static final String STOCK_EXCHANGE = "StockExchangeRateProvider";
    public static final String COIN_MARKET = "CoinMarketRateProvider";

    private RateProviderType() {
        throw new UnsupportedOperationException();
    }

}
