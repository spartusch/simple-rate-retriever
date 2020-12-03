package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Service
class OnVistaRateProvider(
    private val properties: OnVistaProperties,
    private val meterRegistry: MeterRegistry,
    private val httpClient: HttpClient,
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val symbolToUriCache = ConcurrentHashMap<TradeSymbol, URI>()

    private val assetLinkRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()
    private val amountRegex = ("<span class=\"price\">([0-9,.]+) EUR</span>" +
            "|UMRECHNUNGSKURS:\\s*([0-9,.]+) EUR" +
            "|<span data-push[^>]*>([0-9,.]+)</span>\\s*<span[^>]+>EUR</span>").toRegex()

    private fun Regex.extractFirst(
        content: String
    ) = this.find(content)
        ?.groupValues?.asSequence()
        ?.drop(1)
        ?.filterNot { it.isEmpty() }
        ?.first()

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: Currency) = ("EUR" == currency.currencyCode)

    private fun getSearchUri(
        symbol: TradeSymbol
    ) = symbol.map { sym -> "${properties.uri}$sym".toURI() }

    private fun getAssetUri(
        symbol: TradeSymbol
    ) = symbolToUriCache.computeIfAbsent(symbol) {
        httpClient.getUrl(requestTimer, getSearchUri(symbol))
            .let { searchPage -> assetLinkRegex.extractFirst(searchPage) }
            ?.toURI()
            ?: throw DataExtractionException("Asset link not found")
    }

    private fun getCurrentRate(
        retries: Int,
        symbol: TradeSymbol,
        currency: Currency
    ): BigDecimal = try {
        httpClient.getUrl(requestTimer, getAssetUri(symbol), MediaType.TEXT_HTML_VALUE)
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
