package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

class RateServiceImplTest {

    private lateinit var provider: RateProvider
    private lateinit var cut: RateServiceImpl

    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("etf110")
    private val currency = Currency.getInstance("EUR")
    private val locale = Locale.forLanguageTag("de-DE")

    @BeforeEach
    fun setUp() {
        val properties = SimpleRateRetrieverProperties(fractionDigits = 4)
        provider = Mockito.mock(RateProvider::class.java)
        cut = RateServiceImpl(properties, listOf(provider))
    }

    // isRegisteredProviderOrThrow

    @Test
    fun isRegisteredProviderOrThrow_returnsTrueForConfiguredProvider() {
        given(provider.getProviderId()).willReturn(ProviderId("youKnowMe"))
        val ret = cut.isRegisteredProviderOrThrow(ProviderId("youKnowMe"))
        assertThat(ret).isTrue()
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
        cut = RateServiceImpl(SimpleRateRetrieverProperties(0), listOf())
        ThrowableAssert.catchThrowableOfType({
            cut.isRegisteredProviderOrThrow(ProviderId("unknown"))
        }, IllegalArgumentException::class.java)
    }

    // getCurrentRate

    @Test
    fun getCurrentRate_happyCase() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate(providerId, symbol, currency, locale)

        assertThat(rate).isEqualTo("12,3400")
    }

    @Test
    fun getCurrentRate_normalizesToUpperCase() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate(providerId, symbol, currency, locale)

        assertThat(rate).isNotBlank()
    }

    @Test
    fun getCurrentRate_fractionDigitsIsConfigurable() {
        cut = RateServiceImpl(SimpleRateRetrieverProperties(fractionDigits = 6), listOf(provider))
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate(providerId, symbol, currency, locale)

        assertThat(rate).isEqualTo("12,340000")
    }

    @Test
    fun getCurrentRate_fractionDigitsAreRoundedAtTheLastDigit() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willReturn(BigDecimal("12.34567"))

        val rate = cut.getCurrentRate(providerId, symbol, currency, locale)

        assertThat(rate).isEqualTo("12,3457")
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyCodeIsNotSupported() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(false)

        val e = ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate(providerId, symbol, currency, locale)
        }, IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining(currency.displayName) // should report unsupported currency code
    }

    @Test
    fun getCurrentRate_throwsIfFetchingRatesThrowsThrowable() {
        given(provider.getProviderId()).willReturn(providerId)
        given(provider.isCurrencySupported(currency)).willReturn(true)
        given(provider.getCurrentRate(symbol, currency)).willThrow(Error("some error"))

        val e = ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate(providerId, symbol, currency, locale)
        }, RuntimeException::class.java)

        assertThat(e).hasMessageContaining("some error")
    }

    @Test
    fun getCurrentRate_throwsIfProviderIsUnknown() {
        given(provider.getProviderId()).willReturn(providerId)
        ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate(ProviderId("youDontKnowMe"), symbol, currency, locale)
        }, IllegalArgumentException::class.java)
    }
}
