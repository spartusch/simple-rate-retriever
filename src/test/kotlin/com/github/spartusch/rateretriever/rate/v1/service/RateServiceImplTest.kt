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

    private lateinit var provider: RateProvider
    private lateinit var cut: RateServiceImpl

    @BeforeEach
    fun setUp() {
        val properties = SimpleRateRetrieverProperties(fractionDigits = 4)
        provider = Mockito.mock(RateProvider::class.java)
        cut = RateServiceImpl(properties, listOf(provider))
    }

    // isRegisteredProviderOrThrow

    @Test
    fun isRegisteredProviderOrThrow_returnsTrueForConfiguredProvider() {
        given(provider.getProviderId()).willReturn("youKnowMe")
        val ret = cut.isRegisteredProviderOrThrow("youKnowMe")
        assertThat(ret).isTrue()
    }

    @Test
    fun isRegisteredProviderOrThrow_returnsTrueForConfiguredProviderIgnoringCase() {
        given(provider.getProviderId()).willReturn("YoUkNoWmE")
        val ret = cut.isRegisteredProviderOrThrow("youKNOWme")
        assertThat(ret).isTrue()
    }

    @Test
    fun isRegisteredProviderOrThrow_throwsForUnknownProvider() {
        given(provider.getProviderId()).willReturn("youKnowMe")

        val e = ThrowableAssert.catchThrowableOfType({
            cut.isRegisteredProviderOrThrow("unknown")
        }, NotFoundException::class.java)

        assertThat(e).hasMessageContaining("unknown") // must include unknown id
    }

    @Test
    fun isRegisteredProviderOrThrow_throwsIfNoProvidersAreConfigured() {
        cut = RateServiceImpl(SimpleRateRetrieverProperties(0), listOf())
        ThrowableAssert.catchThrowableOfType({
            cut.isRegisteredProviderOrThrow("unknown")
        }, NotFoundException::class.java)
    }

    // getCurrentRate

    @Test
    fun getCurrentRate_happyCase() {
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(provider.getCurrentRate("SYM", "EUR")).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate("provider", "SYM", "EUR", "de-DE")

        assertThat(rate).isEqualTo("12,3400")
    }

    @Test
    fun getCurrentRate_normalizesToUpperCase() {
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(provider.getCurrentRate("SYM", "EUR")).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate("provider", "sym", "eur", "de-DE")

        assertThat(rate).isNotBlank()
    }

    @Test
    fun getCurrentRate_fractionDigitsIsConfigurable() {
        cut = RateServiceImpl(SimpleRateRetrieverProperties(fractionDigits = 6), listOf(provider))
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(provider.getCurrentRate("SYM", "EUR")).willReturn(BigDecimal("12.34"))

        val rate = cut.getCurrentRate("provider", "SYM", "EUR", "de-DE")

        assertThat(rate).isEqualTo("12,340000")
    }

    @Test
    fun getCurrentRate_fractionDigitsAreRoundedAtTheLastDigit() {
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(provider.getCurrentRate("SYM", "EUR")).willReturn(BigDecimal("12.34567"))

        val rate = cut.getCurrentRate("provider", "SYM", "EUR", "de-DE")

        assertThat(rate).isEqualTo("12,3457")
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyCodeIsNotSupported() {
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported(anyString())).willReturn(false)

        val e = ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate("provider", "SYM", "EUR", "de-DE")
        }, IllegalArgumentException::class.java)

        assertThat(e).hasMessageContaining("EUR") // should report unsupported currency code
    }

    @Test
    fun getCurrentRate_throwsIfFetchingRatesThrowsThrowable() {
        given(provider.getProviderId()).willReturn("provider")
        given(provider.isCurrencyCodeSupported("EUR")).willReturn(true)
        given(provider.getCurrentRate(anyString(), anyString())).willThrow(Error("some error"))

        val e = ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate("provider", "SYM", "EUR", "de-DE")
        }, RuntimeException::class.java)

        assertThat(e).hasMessageContaining("some error")
    }

    @Test
    fun getCurrentRate_throwsIfProviderIsUnknown() {
        given(provider.getProviderId()).willReturn("provider")
        ThrowableAssert.catchThrowableOfType({
            cut.getCurrentRate("youDontKnowMe", "SYM", "EUR", "de-DE")
        }, NotFoundException::class.java)
    }
}
