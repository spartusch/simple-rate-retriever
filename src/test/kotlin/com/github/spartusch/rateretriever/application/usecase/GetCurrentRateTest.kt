package com.github.spartusch.rateretriever.application.usecase

import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.RateProvider
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.utils.rate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import javax.money.Monetary

class GetCurrentRateTest {

    private lateinit var provider: RateProvider
    private lateinit var cut: GetCurrentRate

    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("etf110")
    private val currency = Monetary.getCurrency("EUR")

    @BeforeEach
    fun setUp() {
        provider = Mockito.mock(RateProvider::class.java)
        given(provider.getProviderId()).willReturn(providerId)
        cut = GetCurrentRate(listOf(provider))
    }

    @Test
    fun getCurrentRate_happyCase() {
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency))
            .willReturn(rate("12.34", currency))

        val rate = cut(providerId, symbol, currency)

        assertThat(rate).isEqualTo(rate("12.3400", currency))
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyCodeIsNotSupported() {
        given(provider.isCurrencySupported(currency)).willReturn(false)

        val e = ThrowableAssert.catchThrowableOfType({
            cut(providerId, symbol, currency)
        }, IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining(currency.currencyCode) // should report unsupported currency code
    }

    @Test
    fun getCurrentRate_returnsNullIfFetchingRatesThrowsThrowable() {
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willThrow(Error("some error"))

        val result = cut(providerId, symbol, currency)

        assertThat(result).isNull()
    }

    @Test
    fun getCurrentRate_throwsIfProviderIsUnknown() {
        ThrowableAssert.catchThrowableOfType({
            cut(ProviderId("youDontKnowMe"), symbol, currency)
        }, IllegalArgumentException::class.java)
    }
}
