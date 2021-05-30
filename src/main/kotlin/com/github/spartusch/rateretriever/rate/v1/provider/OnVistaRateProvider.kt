package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.OnVistaProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import io.micrometer.core.instrument.MeterRegistry
import org.javamoney.moneta.Money
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import javax.money.convert.MonetaryConversions

private val log = LoggerFactory.getLogger("OnVistaRateProvider")

@Service
class OnVistaRateProvider(
    private val properties: OnVistaProperties,
    meterRegistry: MeterRegistry,
    private val httpClient: HttpClient
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val symbolToUriCache = ConcurrentHashMap<TickerSymbol, URI>()

    private val assetLinkRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()
    private val priceAndCurrencyInGermanLocaleRegex = ("<span class=\"price\">([0-9,.]+) ([A-Z]{3}|GBp)</span>" +
            "|<data [^>]*>\\s*([0-9,.]+)\\s*<span[^>]*>\\W*([A-Z]{3}|GBp)\\s*</span>").toRegex(RegexOption.MULTILINE)
    private val priceRegex = "<meta property=\"schema:price\" content=\"([0-9,.]+)\">".toRegex()
    private val currencyRegex = "<span property=\"schema:priceCurrency\">(\\w+)</span>".toRegex()

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: CurrencyUnit) =
        MonetaryConversions.isConversionAvailable(currency)

    private fun getSearchUri(symbol: TickerSymbol) =
        symbol.let { "${properties.uri}$it".toURI() }

    private fun getAssetUri(symbol: TickerSymbol) =
        symbolToUriCache.computeIfAbsent(symbol) {
            httpClient.getUrl(requestTimer, getSearchUri(symbol))
                .let { assetLinkRegex.find(it) ?: throw DataExtractionException("Asset link not found") }
                .groupValues[1]
                .toURI()
        }

    @Suppress("MagicNumber")
    private fun parseRate(page: String): Pair<BigDecimal, String> =
        priceAndCurrencyInGermanLocaleRegex.find(page)?.groupValues?.let {
            val rateIndex = if (it[1].isNotBlank()) 1 else 3
            return it[rateIndex].toBigDecimal(Locale.GERMANY) to it[rateIndex + 1]
        } ?: let {
            val rate = priceRegex.find(page)?.groupValues?.get(1)
            val currency = currencyRegex.find(page)?.groupValues?.get(1)
            return if (rate != null && currency != null) {
                rate.toBigDecimal() to currency
            } else {
                log.debug("Failed to extract rate from page: {}", page)
                throw DataExtractionException("Rate of asset not found")
            }
        }

    @Suppress("MagicNumber")
    private fun getCurrentRate(retries: Int, symbol: TickerSymbol, currency: CurrencyUnit): MonetaryAmount =
        try {
            val page = httpClient.getUrl(requestTimer, getAssetUri(symbol), MediaType.TEXT_HTML_VALUE)
            val (rate, rateCurrency) = parseRate(page)
            val conversion = MonetaryConversions.getConversion(currency)

            if ("GBp" == rateCurrency) {
                Money.of(rate.divide(BigDecimal(100)), Monetary.getCurrency("GBP"))
            } else {
                Money.of(rate, Monetary.getCurrency(rateCurrency))
            }.with(conversion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            symbolToUriCache.remove(symbol)
            if (retries > 0) {
                getCurrentRate(retries - 1, symbol, currency)
            } else {
                throw e
            }
        }

    override fun getCurrentRate(symbol: TickerSymbol, currency: CurrencyUnit) =
        getCurrentRate(properties.maxRetries, symbol, currency)
}
