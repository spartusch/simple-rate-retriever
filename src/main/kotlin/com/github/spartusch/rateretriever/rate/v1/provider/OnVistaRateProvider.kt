package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import io.micrometer.core.instrument.Metrics
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.util.Currency
import java.util.Locale

@Service
class OnVistaRateProvider(private val properties: OnVistaProperties) : RateProvider {

    private val providerId = ProviderId(properties.id)
    private val symbolToUriCache = mutableMapOf<TradeSymbol, URI>()
    private val requestTimer = Metrics.timer("provider.requests", "provider.name", "OnVistaRateProvider")
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    private val assetLinkRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()
    private val amountRegex = ("<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|Umrechnung:</a>\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>").toRegex()

    private fun Regex.extractFirst(
        content: String
    ) = this.find(content)
        ?.groupValues?.asSequence()
        ?.drop(1)
        ?.filterNot { it.isEmpty() }
        ?.first()

    override fun getProviderId() = providerId

    override fun isCurrencyCodeSupported(
        currency: Currency
    ) = "EUR" == currency.currencyCode

    private fun getSearchUri(
        symbol: TradeSymbol
    ) = "${properties.uri}$symbol".toURI()

    private fun getAssetUri(
        symbol: TradeSymbol
    ) = symbolToUriCache.computeIfAbsent(symbol) {
        httpClient
            .getUrl(getSearchUri(symbol), MediaType.APPLICATION_JSON_VALUE, requestTimer)
            .let { searchPage -> assetLinkRegex.extractFirst(searchPage) }
            ?.toURI()
            ?: throw DataExtractionException("Asset link not found")
    }

    private fun getCurrentRate(
        retries: Int,
        symbol: TradeSymbol,
        currency: Currency
    ): BigDecimal = try {
        httpClient
            .getUrl(getAssetUri(symbol), MediaType.TEXT_HTML_VALUE, requestTimer)
            .let { assetPage -> amountRegex.extractFirst(assetPage) }
            ?.toBigDecimal(Locale.GERMANY)
            ?: throw DataExtractionException("Amount not found")
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        symbolToUriCache.remove(symbol)
        if (retries > 0) {
            getCurrentRate(retries - 1, symbol, currency)
        } else {
            throw e
        }
    }

    override fun getCurrentRate(
        symbol: TradeSymbol,
        currency: Currency
    ) = getCurrentRate(properties.maxRetries, symbol, currency)
}
