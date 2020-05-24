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
class OnVistaRateProvider(private val properties: OnVistaProperties) : StockExchangeRateProvider {

    private val symbolToUriCache = mutableMapOf<String, URI>()
    private val requestTimer = Metrics.timer("provider.requests", "provider.name", "OnVistaRateProvider")
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    private val assetPageRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()
    private val amountRegex = ("<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|Umrechnung:</a>\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>").toRegex()

    override fun isCurrencyCodeSupported(currencyCode: String) = "EUR".equals(currencyCode, ignoreCase = true)

    private fun Regex.extractFirst(content: String) = this.find(content)
            ?.groupValues?.asSequence()
            ?.drop(1)
            ?.filterNot { it.isEmpty() }
            ?.first()

    private fun getAssetLink(symbol: String) = symbolToUriCache.computeIfAbsent(symbol) {
        val searchUri = properties.uri + symbol
        val searchPage = httpClient.getUrl(searchUri.toURI(), MediaType.APPLICATION_JSON_VALUE, requestTimer)
        assetPageRegex.extractFirst(searchPage)?.toURI() ?: throw DataExtractionException("Asset not found")
    }

    private fun getCurrentRate(retries: Int, symbol: String, currencyCode: String): BigDecimal = try {
        val assetPage = httpClient.getUrl(getAssetLink(symbol), MediaType.TEXT_HTML_VALUE, requestTimer)
        amountRegex.extractFirst(assetPage)?.toBigDecimal(Locale.GERMANY) ?: throw DataExtractionException("Amount not found")
    } catch (e: Exception) {
        symbolToUriCache.remove(symbol)
        if (retries > 0) {
            getCurrentRate(retries - 1, symbol, currencyCode)
        } else {
            throw e
        }
    }

    override fun getCurrentRate(symbol: String, currencyCode: String) = getCurrentRate(properties.maxRetries, symbol, currencyCode)

}
