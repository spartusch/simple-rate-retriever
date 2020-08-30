package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import java.net.http.HttpClient
import java.util.Currency

class OnVistaRateProviderTest {

    private lateinit var properties: OnVistaProperties

    private lateinit var cut: OnVistaRateProvider

    @BeforeEach
    fun setUp() {
        properties = OnVistaProperties("someId", "/api/header/search?q=")
        val httpClient = Mockito.mock(HttpClient::class.java)
        cut = OnVistaRateProvider(properties, SimpleMeterRegistry(), httpClient)
    }

    @Test
    fun getProviderId_returnsConfiguredId() {
        assertThat(cut.getProviderId()).isEqualTo(ProviderId("someId"))
    }

    //  isCurrencySupported

    @ParameterizedTest
    @ValueSource(strings = ["EUR"])
    fun isCurrencySupported_supportedCodes(
        currencyCode: String
    ) {
        assertThat(cut.isCurrencySupported(Currency.getInstance(currencyCode))).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["USD"])
    fun isCurrencySupported_unsupportedCodes(
        currencyCode: String
    ) {
        assertThat(cut.isCurrencySupported(Currency.getInstance(currencyCode))).isFalse()
    }
}
