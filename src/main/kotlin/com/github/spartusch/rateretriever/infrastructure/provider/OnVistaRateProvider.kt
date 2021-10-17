package com.github.spartusch.rateretriever.infrastructure.provider

import com.github.spartusch.rateretriever.application.AbstractTimedRateProvider
import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.Rate
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.infrastructure.provider.exception.DataExtractionException
import com.github.spartusch.rateretriever.infrastructure.provider.extensions.getUrl
import com.github.spartusch.rateretriever.infrastructure.provider.extensions.toBigDecimal
import com.github.spartusch.rateretriever.infrastructure.provider.extensions.toURI
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.javamoney.moneta.Money
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

@Service
class OnVistaRateProvider(
    private val properties: OnVistaProperties,
    private val httpClient: HttpClient,
    meterRegistry: MeterRegistry
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val symbolToUriCache = ConcurrentHashMap<TickerSymbol, URI>()
    private val assetLinkRegex = "\"snapshotlink\":\"([^\"]+)\"".toRegex()

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: CurrencyUnit) = MonetaryConversions.isConversionAvailable(currency)

    private fun getSearchUri(symbol: TickerSymbol) = symbol.let { "${properties.uri}$it".toURI() }

    private fun getAssetUri(symbol: TickerSymbol) = symbolToUriCache.computeIfAbsent(symbol) {
        httpClient.getUrl(requestTimer, getSearchUri(symbol))
            .let { assetLinkRegex.find(it) ?: throw DataExtractionException("Asset link not found") }
            .groupValues[1]
            .toURI()
    }

    @Suppress("MagicNumber")
    private fun convertRateData(rateData: Pair<BigDecimal, String>): MonetaryAmount {
        val (rate, rateCurrency) = rateData
        return if ("GBp" == rateCurrency) {
            Money.of(rate.divide(BigDecimal(100)), Monetary.getCurrency("GBP"))
        } else {
            Money.of(rate, Monetary.getCurrency(rateCurrency))
        }
    }

    @Suppress("MagicNumber")
    private fun getCurrentRate(retries: Int, symbol: TickerSymbol, currency: CurrencyUnit): MonetaryAmount =
        try {
            val page = httpClient.getUrl(requestTimer, getAssetUri(symbol), MediaType.TEXT_HTML_VALUE)
            val conversion = MonetaryConversions.getConversion(currency)
            val rateData = OnVistaRateDataExtractor.extractRateDataFromPageOrThrow(page, symbol)
            convertRateData(rateData).with(conversion)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            symbolToUriCache.remove(symbol)
            if (retries > 0) {
                getCurrentRate(retries - 1, symbol, currency)
            } else {
                throw e
            }
        }

    override fun getCurrentRate(symbol: TickerSymbol, currency: CurrencyUnit) =
        Rate(getCurrentRate(properties.maxRetries, symbol, currency))
}

private object OnVistaRateDataExtractor {
    private object OnVistaJson {
        @Serializable
        data class Quote(
            val isoCurrency: String,
            val last: Double,
        )

        @Serializable
        data class Instrument(
            val name: String,
            val isin: String? = null,
            val wkn: String? = null,
            val symbol: String? = null
        )

        @Serializable
        data class InstrumentItem(
            val instrument: Instrument,
            val quote: Quote
        )

        @Serializable
        data class Snapshot(
            val instrument: Instrument,
            val quote: Quote,
            val relatedInstrumentItemList: List<InstrumentItem> = emptyList()
        )

        @Serializable
        data class Data(
            val snapshot: Snapshot
        )

        @Serializable
        data class PageProperties(
            val data: Data
        )

        @Serializable
        data class Properties(
            val pageProps: PageProperties
        )

        @Serializable
        data class Root(
            val props: Properties
        )

        private val format = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        fun decode(input: String): Snapshot = format.decodeFromString<Root>(input).props.pageProps.data.snapshot
    }

    private fun OnVistaJson.Instrument.hasTickerSymbol(symbol: TickerSymbol): Boolean {
        val lowercaseSymbol = symbol.toString().lowercase()
        return listOf(isin, wkn, this.symbol, name).any { lowercaseSymbol == it?.lowercase() }
    }

    private val jsonRegex = "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.+?)</script>"
        .toRegex(RegexOption.DOT_MATCHES_ALL)
    private val priceRegex = "<meta property=\"schema:price\" content=\"([0-9,.]+)\">"
        .toRegex()
    private val currencyRegex = "<span property=\"schema:priceCurrency\">(\\w+)</span>"
        .toRegex()
    private val priceAndCurrencyInGermanLocaleRegex = ("<span class=\"price\">([0-9,.]+) ([A-Z]{3}|GBp)</span>" +
            "|<data [^>]*>\\s*([0-9,.]+)\\s*<span[^>]*>\\W*([A-Z]{3}|GBp)\\s*</span>"
            ).toRegex(RegexOption.MULTILINE)

    private fun extractRateDataUsingJson(page: String, symbol: TickerSymbol): Pair<BigDecimal, String>? {
        val jsonData = jsonRegex.find(page)?.groupValues?.get(1) ?: return null
        val snapshot = OnVistaJson.decode(jsonData)
        val quote = if (snapshot.instrument.hasTickerSymbol(symbol)) {
            snapshot.quote
        } else {
            snapshot.relatedInstrumentItemList.find { it.instrument.hasTickerSymbol(symbol) }?.quote
        }
        return quote?.let { BigDecimal.valueOf(it.last) to it.isoCurrency }
    }

    private fun extractRateDataUsingSchemaPrice(page: String): Pair<BigDecimal, String>? {
        val rate = priceRegex.find(page)?.groupValues?.get(1)
        val currency = currencyRegex.find(page)?.groupValues?.get(1)
        return if (rate != null && currency != null) {
            rate.toBigDecimal() to currency
        } else {
            null
        }
    }

    @Suppress("MagicNumber")
    private fun extractRateDataUsingGermanLocale(page: String): Pair<BigDecimal, String>? {
        return priceAndCurrencyInGermanLocaleRegex.find(page)?.groupValues?.let {
            val rateIndex = if (it[1].isNotBlank()) 1 else 3
            return it[rateIndex].toBigDecimal(Locale.GERMANY) to it[rateIndex + 1]
        }
    }

    fun extractRateDataFromPageOrThrow(page: String, symbol: TickerSymbol): Pair<BigDecimal, String> =
        extractRateDataUsingJson(page, symbol)
            ?: extractRateDataUsingSchemaPrice(page)
            ?: extractRateDataUsingGermanLocale(page)
            ?: throw DataExtractionException("Failed to extract rate")
}
