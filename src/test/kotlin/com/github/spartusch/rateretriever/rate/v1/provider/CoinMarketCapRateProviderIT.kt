package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.jenspiegsa.wiremockextension.InjectServer
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import com.github.spartusch.rateretriever.rate.WireMockUtils.stubResponse
import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.exception.RequestException
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.net.http.HttpClient
import javax.money.Monetary

@ExtendWith(WireMockExtension::class)
class CoinMarketCapRateProviderIT {

    @InjectServer
    private lateinit var serverMock: WireMockServer

    private lateinit var properties: CoinMarketCapProperties

    private lateinit var cut: CoinMarketCapRateProvider

    private val symbol = TickerSymbol("BTC")
    private val currency = Monetary.getCurrency("EUR")

    @BeforeEach
    fun setUp() {
        properties = CoinMarketCapProperties("id", "${serverMock.baseUrl()}/v1", "apiKey")
        cut = CoinMarketCapRateProvider(properties, SimpleMeterRegistry(), HttpClient.newHttpClient())
    }

    private fun stubCurrencyResponse(currencyCode: String, statusCode: Int): String {
        val data = """{
            "status": {"timestamp": "", "elapsed": 10, "error_code": null, "error_message": null, "credit_count": 1},
            "data": [ {"id": 1, "symbol": "XXX"}, {"id": 2, "symbol": "$currencyCode"} ]
        }""".trimIndent()
        return stubResponse("/v1/fiat/map", data, statusCode)
    }

    private fun stubRateResponse(symbol: TickerSymbol, price: Double, statusCode: Int): String {
        val data = """{
            "status": {"timestamp": "", "elapsed": 10, "error_code": null, "error_message": null, "credit_count": 1},
            "data": {
                "1": {"id": 1, "symbol": "XXX",     "quote": { "2" : {"price": 1337.12345} }},
                "2": {"id": 2, "symbol": "$symbol", "quote": { "2" : {"price": $price} }}
            }
        }""".trimIndent()
        return stubResponse("/v1/cryptocurrency/quotes/latest?symbol=$symbol&convert_id=2", data, statusCode)
    }

    //  isCurrencySupported

    @Test
    fun isCurrencySupported_throwsIfUnauthorized() {
        stubCurrencyResponse(currency.currencyCode, 401)
        ThrowableAssert.catchThrowableOfType({ cut.isCurrencySupported(currency) }, RequestException::class.java)
    }

    @Test
    fun isCurrencySupported() {
        val url = stubCurrencyResponse(currency.currencyCode, 200)

        val response = cut.isCurrencySupported(currency)

        assertThat(response).isTrue()
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(url))
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("X-CMC_PRO_API_KEY", WireMock.equalTo("apiKey")))
    }

    @Test
    fun isCurrencySupported_returnsFalseIfNotSupported() {
        stubCurrencyResponse("FOO", 200)
        val response = cut.isCurrencySupported(currency)
        assertThat(response).isFalse
    }

    @Test
    fun isCurrencySupported_isCachedOnSuccess() {
        val url = stubCurrencyResponse(currency.currencyCode, 200)

        cut.isCurrencySupported(currency)
        cut.isCurrencySupported(currency)

        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(url)))
    }

    @Test
    fun isCurrencySupported_isNotCachedOnFailure() {
        val url = stubCurrencyResponse("FOO", 200)

        cut.isCurrencySupported(currency)
        cut.isCurrencySupported(currency)

        WireMock.verify(2, WireMock.getRequestedFor(WireMock.urlEqualTo(url)))
    }

    //  getCurrentRate

    @Test
    fun getCurrentRate() {
        stubCurrencyResponse(currency.currencyCode, 200)
        val url = stubRateResponse(symbol, 1234.5678, 200)

        val response = cut.getCurrentRate(symbol, currency)

        assertThat(response).isEqualByComparingTo(Money.of(BigDecimal("1234.5678"), currency))
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(url))
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("X-CMC_PRO_API_KEY", WireMock.equalTo("apiKey")))
    }

    @Test
    fun getCurrentRate_throwsIfAmountIsNotFound() {
        stubCurrencyResponse(currency.currencyCode, 200)
        val data = """{
            "status": {"timestamp": "", "elapsed": 10, "error_code": null, "error_message": null, "credit_count": 1},
            "data": {"1": {"id": 1, "symbol": "XXX", "quote": { "2" : {"price": 1337.12345} }}}
        }""".trimIndent()
        stubResponse("/v1/cryptocurrency/quotes/latest?symbol=$symbol&convert_id=2", data, 200)

        ThrowableAssert.catchThrowableOfType({ cut.getCurrentRate(symbol, currency) },
            DataExtractionException::class.java)
    }

    @Test
    fun getCurrentRate_throwsIfCurrencyIsUnsupported() {
        stubCurrencyResponse("FOO", 200)
        ThrowableAssert.catchThrowableOfType({ cut.getCurrentRate(symbol, currency) },
            IllegalArgumentException::class.java)
    }

    @Test
    fun getCurrentRate_throwsIfAnErrorOccurs() {
        stubCurrencyResponse(currency.currencyCode, 200)
        stubRateResponse(symbol, 1234.5678, 500)
        ThrowableAssert.catchThrowableOfType({ cut.getCurrentRate(symbol, currency) },
            RequestException::class.java)
    }
}
