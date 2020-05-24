package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock
import com.github.jenspiegsa.wiremockextension.InjectServer
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import com.github.spartusch.rateretriever.rate.WireMockUtils.stubResponse
import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val SEARCH_URL = "/api/header/search?q=foo"
private const val ASSET_URL = "/some/foo"

@ExtendWith(WireMockExtension::class)
class OnVistaRateProviderIT {

    @InjectServer
    private lateinit var serverMock: WireMockServer

    @ConfigureWireMock
    private val options = wireMockConfig().dynamicPort()

    private lateinit var properties: OnVistaProperties

    private lateinit var cut: OnVistaRateProvider

    @BeforeEach
    fun setUp() {
        properties = OnVistaProperties("${serverMock.baseUrl()}/api/header/search?q=")
        cut = OnVistaRateProvider(properties)
    }

    private fun stubSearchPage(assetLink: String, statusCode: Int = 200) {
        stubResponse(SEARCH_URL, """.."snapshotlink":"$assetLink"..""", statusCode)
    }

    private fun stubAssetPage(suffix: String = "",
                              statusCode: Int = 200,
                              content: String = """..<span class="price">1.230,45 EUR</span>.."""): String {
        return serverMock.baseUrl() + stubResponse(ASSET_URL + suffix, content, statusCode)
    }

    //  getCurrentRate

    @Test
    fun getCurrentRate_searchAndRetrieve() {
        stubSearchPage(assetLink = stubAssetPage())

        cut.getCurrentRate("foo", "EUR")

        WireMock.verify(1, getRequestedFor(urlEqualTo(SEARCH_URL)))
        WireMock.verify(1, getRequestedFor(urlEqualTo(ASSET_URL)))
    }

    @Test
    fun getCurrentRate_searchIsCached() {
        stubSearchPage(assetLink = stubAssetPage())

        cut.getCurrentRate("foo", "EUR")
        cut.getCurrentRate("foo", "EUR")
        cut.getCurrentRate("foo", "EUR")

        WireMock.verify(1, getRequestedFor(urlEqualTo(SEARCH_URL)))
        WireMock.verify(3, getRequestedFor(urlEqualTo(ASSET_URL)))
    }

    @Test
    fun getCurrentRate_searchIsNotCachedOnFailure() {
        stubSearchPage(assetLink = stubAssetPage(statusCode = 404))

        ThrowableAssert.catchThrowable { cut.getCurrentRate("foo", "EUR") }
        ThrowableAssert.catchThrowable { cut.getCurrentRate("foo", "EUR") }
        ThrowableAssert.catchThrowable { cut.getCurrentRate("foo", "EUR") }

        val expectedRequests = 3 * (1 + properties.maxRetries)
        WireMock.verify(expectedRequests, getRequestedFor(urlEqualTo(SEARCH_URL)))
        WireMock.verify(expectedRequests, getRequestedFor(urlEqualTo(ASSET_URL)))
    }

    @Test
    fun getCurrentRate_searchThrowsExceptionIfAssetUrlCannotBeExtracted() {
        stubResponse(SEARCH_URL, "no link to extract - sorry!", 200)

        val exception =
                ThrowableAssert.catchThrowableOfType({ cut.getCurrentRate("foo", "EUR") }, DataExtractionException::class.java)

        assertThat(exception).hasMessageContaining("Asset not found")
    }

    @Test
    fun getCurrentRate_throwsExceptionIfAmountCannotBeExtracted() {
        stubSearchPage(assetLink = stubAssetPage(content = "no amount to extract in here"))

        val exception =
                ThrowableAssert.catchThrowableOfType({ cut.getCurrentRate("foo", "EUR") }, DataExtractionException::class.java)

        assertThat(exception).hasMessageContaining("Amount not found")
    }

    @Test
    fun getCurrentRate_extractsFirstEurRate() {
        stubSearchPage(assetLink =
            stubAssetPage(content =
                """<span class="price">100,1234 USD</span>
                <span class="price">111,2200 EUR</span>
                <a>Umrechnung:</a> 333,4400 EUR"""
        ))

        val rate = cut.getCurrentRate("foo", "EUR")

         assertThat(rate).isEqualByComparingTo("111.22")
    }

    @Test
    fun getCurrentRate_extractsConvertedRateIfRateIsNonEur() {
        stubSearchPage(assetLink =
            stubAssetPage(content =
                """<span class="price">123,00 USD</span><a>Umrechnung:</a> 150,00 EUR"""
        ))

        val rate = cut.getCurrentRate("foo", "EUR")

        assertThat(rate).isEqualByComparingTo("150")
    }

    @Test
    fun getCurrentRate_extractsDataPushRateInEur() {
        stubSearchPage(assetLink =
            stubAssetPage(content =
                """<span data-push att='a' att="b">999,99</span> 
                <span att='c'>USD</span>
                <span data-push att='a' att="b">12.345,12</span>
                <span att='c'>EUR</span>"""
        ))

        val rate = cut.getCurrentRate("foo", "EUR")

        assertThat(rate).isEqualByComparingTo("12345.12")
    }

}
