package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Currency

class OnVistaRateProviderTest {

    private lateinit var properties: OnVistaProperties

    private lateinit var cut: OnVistaRateProvider

    @BeforeEach
    fun setUp() {
        properties = OnVistaProperties("someId", "/api/header/search?q=")
        cut = OnVistaRateProvider(properties)
    }

    @Test
    fun getProviderId_returnsConfiguredId() {
        assertThat(cut.getProviderId()).isEqualTo(ProviderId("someId"))
    }

    //  isCurrencyCodeSupported

    @ParameterizedTest
    @ValueSource(strings = ["EUR"])
    fun isCurrencyCodeSupported_supportedCodes(
        currencyCode: String
    ) {
        assertThat(cut.isCurrencyCodeSupported(Currency.getInstance(currencyCode))).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["USD"])
    fun isCurrencyCodeSupported_unsupportedCodes(
        currencyCode: String
    ) {
        assertThat(cut.isCurrencyCodeSupported(Currency.getInstance(currencyCode))).isFalse()
    }
}
