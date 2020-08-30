package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.http.HttpClient

class CoinMarketCapRateProviderTest {

    private lateinit var properties: CoinMarketCapProperties
    private lateinit var httpClient: HttpClient

    private lateinit var cut: CoinMarketCapRateProvider

    @BeforeEach
    fun setUp() {
        properties = CoinMarketCapProperties("someId", "/api", "apiKey")
        httpClient = Mockito.mock(HttpClient::class.java)
        cut = CoinMarketCapRateProvider(properties, SimpleMeterRegistry(), httpClient)
    }

    @Test
    fun getProviderId_returnsConfiguredId() {
        assertThat(cut.getProviderId()).isEqualTo(ProviderId("someId"))
    }
}
