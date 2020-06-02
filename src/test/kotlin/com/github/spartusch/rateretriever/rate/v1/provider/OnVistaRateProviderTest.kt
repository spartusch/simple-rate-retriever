package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource

class OnVistaRateProviderTest {

    private lateinit var properties: OnVistaProperties

    private lateinit var cut: OnVistaRateProvider

    @BeforeEach
    fun setUp() {
        properties = OnVistaProperties("/api/header/search?q=")
        cut = OnVistaRateProvider(properties)
    }

    //  isCurrencyCodeSupported

    @ParameterizedTest
    @ValueSource(strings = ["EUR", "eur", "Eur", "eUr", "euR"])
    fun isCurrencyCodeSupported_supportedCodes(currencyCode: String) {
        assertThat(cut.isCurrencyCodeSupported(currencyCode)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["USD", "XYZ"])
    @EmptySource
    fun isCurrencyCodeSupported_unsupportedCodes(currencyCode: String) {
        assertThat(cut.isCurrencyCodeSupported(currencyCode)).isFalse()
    }
}
