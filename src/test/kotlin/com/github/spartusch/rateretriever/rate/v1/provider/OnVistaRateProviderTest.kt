package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import javax.money.CurrencyUnit
import javax.money.Monetary

class OnVistaRateProviderTest {

    private lateinit var properties: OnVistaProperties
    private lateinit var httpClient: HttpClient

    private lateinit var cut: OnVistaRateProvider

    companion object {
        const val assetUrl = "http://asset"
        const val searchPageContent = "\"snapshotlink\":\"$assetUrl\""

        fun searchUrl(symbol: String) = "http://search?q=$symbol"

        fun assetPageContents(currency: String) = listOf(
            "foo <span property=\"schema:priceCurrency\">$currency</span>\n" +
                    "<meta property=\"schema:price\" content=\"50.0412\"> bar",
            "foo <span class=\"price\">50,0412 $currency</span> bar",
            "foo <data class=\"text-nowrap\" value=\"50.0412\">\n" +
                    " 50,0412\n <span>\n <!-- -->\n $currency\n </span>\n </data> bar"
        )

        @JvmStatic
        fun allMonetaryCurrenciesProvider() = Monetary.getCurrencies("default").map { Arguments.of(it) }

        @JvmStatic
        fun assetPageContentsProvider() = assetPageContents("EUR").map { Arguments.of(it) }
    }

    @BeforeEach
    fun setUp() {
        mockkStatic("com.github.spartusch.rateretriever.rate.v1.provider.HttpClientExtensionsKt")
        properties = OnVistaProperties("someId", "http://search?q=", 3)
        httpClient = mockk()
        cut = OnVistaRateProvider(properties, SimpleMeterRegistry(), httpClient)
    }

    @Test
    fun getProviderId_returnsConfiguredId() {
        assertThat(cut.getProviderId()).isEqualTo(ProviderId("someId"))
    }

    @ParameterizedTest
    @MethodSource("allMonetaryCurrenciesProvider")
    fun isCurrencySupported_supportedCodes(currency: CurrencyUnit) {
        assertThat(cut.isCurrencySupported(currency)).isTrue
    }

    private fun mockHttpClient(url: String, mediaType: String, response: String) {
        every { httpClient.getUrl(any(), URI(url), mediaType) } returns response
    }

    @ParameterizedTest
    @MethodSource("assetPageContentsProvider")
    fun getCurrentRate_parsesRates(page: String) {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, page)

        val result = cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))

        assertThat(result).isEqualTo(Money.of(BigDecimal("50.0412"), Monetary.getCurrency("EUR")))
    }

    @Test
    fun getCurrentRate_parsesGBp() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, assetPageContents("GBp")[0])

        val result = cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("GBP"))

        assertThat(result).isEqualTo(Money.of(BigDecimal("0.500412"), Monetary.getCurrency("GBP")))
    }

    @Test
    fun getCurrentRate_convertsCurrencies() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, assetPageContents("USD")[0])

        val result = cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))

        assertThat(result.currency).isEqualTo(Monetary.getCurrency("EUR"))
    }

    @Test
    fun getCurrentRate_throwsExtractionErrorIfRateCannotBeParsed() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, "no parsable content here")

        val e = ThrowableAssert.catchThrowable {
            cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("GBP"))
        }

        assertThat(e.javaClass).isAssignableFrom(DataExtractionException::class.java)
        assertThat(e).hasMessageContaining("Rate")
    }

    @Test
    fun getCurrentRate_throwsExtractionErrorIfAssetLinkCannotBeFound() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, "no parsable content here")

        val e = ThrowableAssert.catchThrowable {
            cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("GBP"))
        }

        assertThat(e.javaClass).isAssignableFrom(DataExtractionException::class.java)
        assertThat(e).hasMessageContaining("Asset")
    }

    @Test
    fun getCurrentRate_cachesSearchResult() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, assetPageContents("EUR")[0])

        cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))
        cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))
        cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))

        verify(exactly = 1) { httpClient.getUrl(any(), URI(searchUrl("SYM")), MediaType.APPLICATION_JSON_VALUE) }
        verify(exactly = 3) { httpClient.getUrl(any(), URI(assetUrl), MediaType.TEXT_HTML_VALUE) }
        confirmVerified(httpClient)
    }

    @Test
    fun getCurrentRate_retriesOnErrors() {
        mockHttpClient(searchUrl("SYM"), MediaType.APPLICATION_JSON_VALUE, searchPageContent)
        mockHttpClient(assetUrl, MediaType.TEXT_HTML_VALUE, "no parseable content here")

        ThrowableAssert.catchThrowable {
            cut.getCurrentRate(TickerSymbol("SYM"), Monetary.getCurrency("EUR"))
        }

        verify(exactly = properties.maxRetries + 1) {
            httpClient.getUrl(any(), URI(searchUrl("SYM")), MediaType.APPLICATION_JSON_VALUE)
            httpClient.getUrl(any(), URI(assetUrl), MediaType.TEXT_HTML_VALUE)
        }
        confirmVerified(httpClient)
    }
}
