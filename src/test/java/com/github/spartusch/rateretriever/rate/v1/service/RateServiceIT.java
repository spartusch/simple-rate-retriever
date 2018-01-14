package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider;
import com.github.spartusch.rateretriever.rate.v1.provider.RateProviderType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RateServiceIT {

    @Autowired
    private RateService rateService;

    @MockBean
    @Qualifier(RateProviderType.STOCK_EXCHANGE)
    private RateProvider stockExchangeRateProvider;

    @MockBean
    @Qualifier(RateProviderType.COIN_MARKET)
    private RateProvider coinMarketRateProvider;

    @Test
    public void test_contextLoads() {
    }

    @Test
    public void test_getStockExchangeRate_isCached() {
        when(stockExchangeRateProvider.isCurrencyCodeSupported("EUR")).thenReturn(Mono.just(true));
        when(stockExchangeRateProvider.getCurrentRate("ETF110", "EUR")).thenReturn(Mono.just(BigDecimal.TEN));

        rateService.getStockExchangeRate("ETF110", "EUR", "de-DE");
        rateService.getStockExchangeRate("ETF110", "EUR", "de-DE");
        rateService.getStockExchangeRate("ETF110", "EUR", "de-DE");

        verify(stockExchangeRateProvider, times(1)).isCurrencyCodeSupported("EUR");
        verify(stockExchangeRateProvider, times(1)).getCurrentRate("ETF110", "EUR");
    }

    @Test
    public void test_getStockExchangeRate_cachedResultsAreNotMixedUp() {
        when(stockExchangeRateProvider.isCurrencyCodeSupported("EUR")).thenReturn(Mono.just(true));
        when(stockExchangeRateProvider.getCurrentRate("ETF110", "EUR")).thenReturn(Mono.just(BigDecimal.TEN));
        when(stockExchangeRateProvider.getCurrentRate("ETF127", "EUR")).thenReturn(Mono.just(BigDecimal.ONE));

        rateService.getStockExchangeRate("ETF110", "EUR", "de-DE");
        rateService.getStockExchangeRate("ETF127", "EUR", "de-DE");

        StepVerifier.create(rateService.getStockExchangeRate("ETF110", "EUR", "de-DE"))
                .expectNext("10,0000")
                .verifyComplete();
        StepVerifier.create(rateService.getStockExchangeRate("ETF127", "EUR", "de-DE"))
                .expectNext("1,0000")
                .verifyComplete();
    }

    @Test
    public void test_getCoinMarketRate_isCached() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Mono.just(BigDecimal.TEN));

        rateService.getCoinMarketRate("bitcoin", "USD", "de-DE");
        rateService.getCoinMarketRate("bitcoin", "USD", "de-DE");
        rateService.getCoinMarketRate("bitcoin", "USD", "de-DE");

        verify(coinMarketRateProvider, times(1)).isCurrencyCodeSupported("USD");
        verify(coinMarketRateProvider, times(1)).getCurrentRate("bitcoin", "USD");
    }

    @Test
    public void test_getCoinMarketRate_cachedResultsAreNotMixedUp() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Mono.just(BigDecimal.TEN));
        when(coinMarketRateProvider.getCurrentRate("ripple", "USD")).thenReturn(Mono.just(BigDecimal.ONE));

        rateService.getCoinMarketRate("bitcoin", "USD", "de-DE");
        rateService.getCoinMarketRate("ripple", "USD", "de-DE");

        StepVerifier.create(rateService.getCoinMarketRate("bitcoin", "USD", "de-DE"))
                .expectNext("10,0000")
                .verifyComplete();
        StepVerifier.create(rateService.getCoinMarketRate("ripple", "USD", "de-DE"))
                .expectNext("1,0000")
                .verifyComplete();
    }
}
