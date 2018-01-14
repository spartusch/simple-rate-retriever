package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RateServiceImplTest {

    private RateProvider stockExchangeRateProvider;
    private RateProvider coinMarketRateProvider;

    private RateServiceImpl rateService;

    @Before
    public void setUp() {
        stockExchangeRateProvider = Mockito.mock(RateProvider.class);
        coinMarketRateProvider = Mockito.mock(RateProvider.class);
        rateService = new RateServiceImpl(stockExchangeRateProvider, coinMarketRateProvider);
    }

    //
    // getCurrentRate
    //

    @Test
    public void test_getCurrentRate_happyCase_localeEnglish() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Mono.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "en-US").block();
        assertThat(result).isEqualTo("10.0000");
    }

    @Test
    public void test_getCurrentRate_happyCase_localeGerman() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("EUR")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("ripple", "EUR")).thenReturn(Mono.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "ripple", "EUR", "de-DE").block();
        assertThat(result).isEqualTo("10,0000");
    }

    @Test
    public void test_getCurrentRate_localeUnknown() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Mono.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "nonsense").block();
        assertThat(result).isEqualTo("10.0000");
    }

    @Test
    public void test_getCurrentRate_currencyCodeUnsupported() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("XXX")).thenReturn(Mono.just(false));
        when(coinMarketRateProvider.getCurrentRate(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "XXX", "en-US"))
                .verifyError(IllegalArgumentException.class);
    }

    @Test
    public void test_getCurrentRate_exceptionInProcessing() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD"))
                .thenReturn(Mono.error(new RuntimeException("Exception message1")));
        StepVerifier.create(rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "de-DE"))
                .verifyErrorMessage("Exception message1");
    }

    @Test
    public void test_getCurrentRate_errorInProcessing() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD"))
                .thenReturn(Mono.error(new RuntimeException("Exception message2")));
        StepVerifier.create(rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "de-DE"))
                .verifyErrorMessage("Exception message2");
    }

    //
    // getCoinMarketRate
    //

    @Test
    public void test_getCoinMarketRate_callsCoinMarketRateProvider() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Mono.just(BigDecimal.TEN));

        rateService.getCoinMarketRate("bitcoin", "USD", "en-US");

        verify(coinMarketRateProvider, times(1)).isCurrencyCodeSupported("USD");
        verify(coinMarketRateProvider, times(1)).getCurrentRate("bitcoin", "USD");
    }

    //
    // getStockExchangeRate
    //

    @Test
    public void test_getStockExchangeRate_callsStockExchangeRateProvider() {
        when(stockExchangeRateProvider.isCurrencyCodeSupported("USD")).thenReturn(Mono.just(true));
        when(stockExchangeRateProvider.getCurrentRate("ETF110", "USD")).thenReturn(Mono.just(BigDecimal.TEN));

        rateService.getStockExchangeRate("ETF110", "USD", "en-US");

        verify(stockExchangeRateProvider, times(1)).isCurrencyCodeSupported("USD");
        verify(stockExchangeRateProvider, times(1)).getCurrentRate("ETF110", "USD");
    }
}
