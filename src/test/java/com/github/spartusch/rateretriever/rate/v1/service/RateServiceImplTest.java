package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Maybe.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "en-US");
        assertThat(result).isEqualTo("10.0000");
    }

    @Test
    public void test_getCurrentRate_happyCase_localeGerman() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("EUR")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("ripple", "EUR")).thenReturn(Maybe.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "ripple", "EUR", "de-DE");
        assertThat(result).isEqualTo("10,0000");
    }

    @Test
    public void test_getCurrentRate_localeUnknown() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Maybe.just(BigDecimal.TEN));
        final String result = rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "nonsense");
        assertThat(result).isEqualTo("10.0000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getCurrentRate_currencyCodeUnsupported() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("XXX")).thenReturn(false);
        rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "XXX", "en-US");
    }

    @Test
    public void test_getCurrentRate_exceptionInProcessing() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD"))
                .thenReturn(Maybe.fromCallable(() -> { throw new RuntimeException("Exception message1"); }));

        final Throwable throwable = catchThrowable(() -> {
            rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "de-DE");
        });

        assertThat(throwable).hasMessage("Exception message1");
    }

    @Test
    public void test_getCurrentRate_errorInProcessing() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD"))
                .thenReturn(Maybe.error(new RuntimeException("Exception message2")));

        final Throwable throwable = catchThrowable(() -> {
            rateService.getCurrentRate(coinMarketRateProvider, "bitcoin", "USD", "de-DE");
        });

        assertThat(throwable).hasMessage("Exception message2");
    }

    //
    // getCoinMarketRate
    //

    @Test
    public void test_getCoinMarketRate_callsCoinMarketRateProvider() {
        when(coinMarketRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(coinMarketRateProvider.getCurrentRate("bitcoin", "USD")).thenReturn(Maybe.just(BigDecimal.TEN));

        rateService.getCoinMarketRate("bitcoin", "USD", "en-US");

        verify(coinMarketRateProvider, times(1)).isCurrencyCodeSupported("USD");
        verify(coinMarketRateProvider, times(1)).getCurrentRate("bitcoin", "USD");
    }

    //
    // getStockExchangeRate
    //

    @Test
    public void test_getStockExchangeRate_callsStockExchangeRateProvider() {
        when(stockExchangeRateProvider.isCurrencyCodeSupported("USD")).thenReturn(true);
        when(stockExchangeRateProvider.getCurrentRate("ETF110", "USD")).thenReturn(Maybe.just(BigDecimal.TEN));

        rateService.getStockExchangeRate("ETF110", "USD", "en-US");

        verify(stockExchangeRateProvider, times(1)).isCurrencyCodeSupported("USD");
        verify(stockExchangeRateProvider, times(1)).getCurrentRate("ETF110", "USD");
    }
}
