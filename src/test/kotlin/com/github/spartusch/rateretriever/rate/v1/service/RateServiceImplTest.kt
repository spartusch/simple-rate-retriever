package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.math.BigDecimal

class RateServiceImplTest {

    private lateinit var firstProvider: StockExchangeRateProvider
    private lateinit var secondProvider: CoinMarketRateProvider
    private lateinit var cut: RateServiceImpl

    @BeforeEach
    fun setUp() {
        firstProvider = Mockito.mock(StockExchangeRateProvider::class.java)
        secondProvider = Mockito.mock(CoinMarketRateProvider::class.java)
        cut = RateServiceImpl(firstProvider, secondProvider)
    }

    @Test
    fun getCurrentRate_happyCase() {
        given(firstProvider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(firstProvider.getCurrentRate("SYM", "EUR")).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate(firstProvider, "SYM", "EUR", "de-DE")

        assertThat(rate).isEqualTo("12,3400")
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyCodeIsNotSupported() {
        given(firstProvider.isCurrencyCodeSupported(anyString())).willReturn(false)

        val e = ThrowableAssert.catchThrowableOfType(
                { cut.getCurrentRate(firstProvider, "SYM", "EUR", "de-DE") },
                IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining("EUR") // should report unsupported currency code
    }

    @Test
    fun getCurrentRate_throwsIfFetchingRatesThrowsThrowable() {
        given(firstProvider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(firstProvider.getCurrentRate(anyString(), anyString())).willThrow(Error("some error"))

        val e = ThrowableAssert.catchThrowableOfType(
                { cut.getCurrentRate(firstProvider, "SYM", "EUR", "de-DE") },
                RuntimeException::class.java)

        assertThat(e).hasMessageContaining("some error")
    }

    @Test
    fun getStockExchangeRate_callsFirstProvider() {
        given(firstProvider.isCurrencyCodeSupported(anyString())).willReturn(true)
        given(firstProvider.getCurrentRate(anyString(), anyString())).willReturn(BigDecimal("12.34"))

        cut.getStockExchangeRate("SYM", "EUR", "en-US")

        verify(firstProvider, times(1)).isCurrencyCodeSupported("EUR")
        verify(firstProvider, times(1)).getCurrentRate("SYM", "EUR")
    }

    @Test
    fun getCoinMarketRate_callsSecondProvider() {
        given(secondProvider.isCurrencyCodeSupported(anyString())).willReturn(true)
        given(secondProvider.getCurrentRate(anyString(), anyString())).willReturn(BigDecimal("12.34"))

        cut.getCoinMarketRate("SYM", "EUR", "en-US")

        verify(secondProvider, times(1)).isCurrencyCodeSupported("EUR")
        verify(secondProvider, times(1)).getCurrentRate("SYM", "EUR")
    }

}
