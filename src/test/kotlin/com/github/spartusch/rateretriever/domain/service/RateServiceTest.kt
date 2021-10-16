package com.github.spartusch.rateretriever.domain.service

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

class RateServiceTest {

    private lateinit var provider: RateProvider
    private lateinit var cut: RateService

    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("etf110")
    private val currency = Monetary.getCurrency("EUR")

    @BeforeEach
    fun setUp() {
        provider = Mockito.mock(RateProvider::class.java)
        cut = RateService(listOf(provider))
    }

    // isRegisteredProviderOrThrow

    @Test
    fun isRegisteredProviderOrThrow_returnsTrueForConfiguredProvider() {
        given(provider.getProviderId()).willReturn(ProviderId("youKnowMe"))
        val ret = cut.isRegisteredProviderOrThrow(ProviderId("youKnowMe"))
        assertThat(ret).isTrue
    }

    @Test
    fun isRegisteredProviderOrThrow_throwsForUnknownProvider() {
        given(provider.getProviderId()).willReturn(ProviderId("youKnowMe"))

        val e = ThrowableAssert.catchThrowableOfType({
            cut.isRegisteredProviderOrThrow(ProviderId("unknown"))
        }, IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining("unknown") // must include unknown id
    }

    @Test
    fun isRegisteredProviderOrThrow_throwsIfNoProvidersAreConfigured() {
        cut = RateService(listOf())
        ThrowableAssert.catchThrowableOfType({
            cut.isRegisteredProviderOrThrow(ProviderId("unknown"))
        }, IllegalArgumentException::class.java)
    }

    // getCurrentRate

    @Test
    fun getCurrentRate_happyCase() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency))
            .willReturn(rate("12.34", currency))

        val rate = cut.getCurrentRate(providerId, symbol, currency)

        assertThat(rate).isEqualTo(rate("12.3400", currency))
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyCodeIsNotSupported() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(false)

        val e = ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate(providerId, symbol, currency)
        }, IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining(currency.currencyCode) // should report unsupported currency code
    }

    @Test
    fun getCurrentRate_returnsNullIfFetchingRatesThrowsThrowable() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willThrow(Error("some error"))

        val result = cut.getCurrentRate(providerId, symbol, currency)

        assertThat(result).isNull()
    }

    @Test
    fun getCurrentRate_throwsIfProviderIsUnknown() {
        given(provider.getProviderId()).willReturn(providerId)
        ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate(ProviderId("youDontKnowMe"), symbol, currency)
        }, IllegalArgumentException::class.java)
    }
}
