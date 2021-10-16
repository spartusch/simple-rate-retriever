package com.github.spartusch.rateretriever.infrastructure.provider

import com.github.spartusch.rateretriever.application.AbstractTimedRateProvider
import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.Rate
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.infrastructure.provider.exception.DataExtractionException
import com.github.spartusch.rateretriever.infrastructure.provider.extensions.getUrl
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.javamoney.moneta.Money
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.money.CurrencyUnit

private object CmcJson {
    @Serializable
    data class Status(
        val timestamp: String,
        val elapsed: Long,
        @SerialName("error_code")
        val errorCode: Long?,
        @SerialName("error_message")
        val errorMessage: String?,
        @SerialName("credit_count")
        val creditCount: Long
    )

    @Serializable
    data class ListResponse<T>(
        val status: Status,
        val data: List<T>
    )

    @Serializable
    data class MapResponse<T>(
        val status: Status,
        val data: Map<String, T>
    )

    @Serializable
    data class FiatCurrency(
        val id: Long,
        val symbol: String,
    )

    @Serializable
    data class CryptoCurrency(
        val id: Long,
        val symbol: String,
        @SerialName("quote")
        val quotes: Map<Long, Quote>
    )

    @Serializable
    data class Quote(
        val price: Double
    )

    private val format = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T> decode(input: String): T = format.decodeFromString(input)
}

@Service
class CoinMarketCapRateProvider(
    properties: CoinMarketCapProperties,
    meterRegistry: MeterRegistry,
    private val httpClient: HttpClient,
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val currencyToCmcIdCache = ConcurrentHashMap<CurrencyUnit, Long?>()

    private val apiKeyHeader = listOf("X-CMC_PRO_API_KEY", properties.apiKey)
    private val fiatEndpoint = URI("${properties.uri}/fiat/map")
    private val rateEndpoint = URI("${properties.uri}/cryptocurrency/quotes/latest")

    private fun getCurrencyId(currency: CurrencyUnit): Long? =
        currencyToCmcIdCache.computeIfAbsent(currency) {
            httpClient.getUrl(requestTimer, fiatEndpoint, additionalHeaders = apiKeyHeader)
                .let { CmcJson.decode<CmcJson.ListResponse<CmcJson.FiatCurrency>>(it).data }
                .find { it.symbol == currency.currencyCode }
                ?.id
        }

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: CurrencyUnit) = (getCurrencyId(currency) != null)

    override fun getCurrentRate(
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): Rate? {
        val currencyId = getCurrencyId(currency) ?: throw IllegalArgumentException("Unsupported currency '$currency'")
        val uri = URI("$rateEndpoint?symbol=$symbol&convert_id=$currencyId")
        return httpClient.getUrl(requestTimer, uri, additionalHeaders = apiKeyHeader)
            .let { CmcJson.decode<CmcJson.MapResponse<CmcJson.CryptoCurrency>>(it).data.values }
            .asSequence()
            .filter { TickerSymbol(it.symbol) == symbol }
            .map { it.quotes[currencyId] }
            .filterNotNull()
            .map { Rate(Money.of(it.price, currency)) }
            .firstOrNull() ?: throw DataExtractionException("Amount not found")
    }
}
