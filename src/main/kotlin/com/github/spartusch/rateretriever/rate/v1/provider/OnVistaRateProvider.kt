package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import io.micrometer.core.instrument.Metrics
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.util.Locale

@Service
class OnVistaRateProvider(private val properties: OnVistaProperties) : RateProvider {

    private val symbolToUriCache = mutableMapOf<String, URI>()
    private val requestTimer = Metrics.timer("provider.requests", "provider.name", "OnVistaRateProvider")
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    private val assetLinkRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()
    private val amountRegex = ("<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|Umrechnung:</a>\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>").toRegex()

    private fun Regex.extractFirst(content: String) =
            this.find(content)
            ?.groupValues?.asSequence()
            ?.drop(1)
            ?.filterNot { it.isEmpty() }
            ?.first()

    override fun getProviderId() = properties.id

    override fun isCurrencyCodeSupported(currencyCode: String) = "EUR".equals(currencyCode, ignoreCase = true)

    private fun getSearchUri(symbol: String) = "${properties.uri}$symbol".toURI()

    private fun getAssetUri(symbol: String) = symbolToUriCache.computeIfAbsent(symbol) {
        httpClient
            .getUrl(getSearchUri(symbol), MediaType.APPLICATION_JSON_VALUE, requestTimer)
            .let { searchPage -> assetLinkRegex.extractFirst(searchPage) }
            ?.toURI()
            ?: throw DataExtractionException("Asset link not found")
    }

    private fun getCurrentRate(retries: Int, symbol: String, currencyCode: String): BigDecimal = try {
        httpClient
            .getUrl(getAssetUri(symbol), MediaType.TEXT_HTML_VALUE, requestTimer)
            .let { assetPage -> amountRegex.extractFirst(assetPage) }
            ?.toBigDecimal(Locale.GERMANY)
            ?: throw DataExtractionException("Amount not found")
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        symbolToUriCache.remove(symbol)
        if (retries > 0) {
            getCurrentRate(retries - 1, symbol, currencyCode)
        } else {
            throw e
        }
    }

    override fun getCurrentRate(symbol: String, currencyCode: String) =
        getCurrentRate(properties.maxRetries, symbol, currencyCode)
}
