package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
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
import javax.money.MonetaryAmount

@Serializable
private data class CmcStatus(
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
private data class CmcListResponse<T>(
    val status: CmcStatus,
    val data: List<T>
)

@Serializable
private data class CmcMapResponse<T>(
    val status: CmcStatus,
    val data: Map<String, T>
)

@Serializable
private data class FiatCurrency(
    val id: Long,
    val symbol: String,
)

@Serializable
private data class CryptoCurrency(
    val id: Long,
    val symbol: String,
    @SerialName("quote")
    val quotes: Map<Long, Quote>
)

@Serializable
private data class Quote(
    val price: Double
)

@Service
class CoinMarketCapRateProvider(
    properties: CoinMarketCapProperties,
    meterRegistry: MeterRegistry,
    private val httpClient: HttpClient,
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val currencyToCmcIdCache = ConcurrentHashMap<CurrencyUnit, Long?>()

    private val format = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val apiKeyHeader = listOf("X-CMC_PRO_API_KEY", properties.apiKey)
    private val fiatEndpoint = URI("${properties.uri}/fiat/map")
    private val rateEndpoint = URI("${properties.uri}/cryptocurrency/quotes/latest")

    private fun getCurrencyId(currency: CurrencyUnit): Long? =
        currencyToCmcIdCache.computeIfAbsent(currency) {
            httpClient.getUrl(requestTimer, fiatEndpoint, additionalHeaders = apiKeyHeader)
                .let { format.decodeFromString<CmcListResponse<FiatCurrency>>(it).data }
                .find { it.symbol == currency.currencyCode }
                ?.id
        }

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: CurrencyUnit) = (getCurrencyId(currency) != null)

    override fun getCurrentRate(
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): MonetaryAmount? {
        val currencyId = getCurrencyId(currency) ?: throw IllegalArgumentException("Unsupported currency '$currency'")
        val uri = URI("$rateEndpoint?symbol=$symbol&convert_id=$currencyId")
        return httpClient.getUrl(requestTimer, uri, additionalHeaders = apiKeyHeader)
            .let { format.decodeFromString<CmcMapResponse<CryptoCurrency>>(it).data.values }
            .asSequence()
            .filter { TickerSymbol(it.symbol) == symbol }
            .map { it.quotes[currencyId] }
            .filterNotNull()
            .map { Money.of(it.price, currency) }
            .firstOrNull() ?: throw DataExtractionException("Amount not found")
    }
}
