package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import com.github.spartusch.rateretriever.rate.v1.exception.DataExtractionException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.util.Currency

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
    val quotes: Map<String, Quote>
)

@Serializable
private data class Quote(
    val price: Double
)

@Service
class CoinMarketCapRateProvider(
    private val properties: CoinMarketCapProperties,
    private val meterRegistry: MeterRegistry,
    private val httpClient: HttpClient,
) : AbstractTimedRateProvider(meterRegistry) {

    private val providerId = ProviderId(properties.id)
    private val currencyToCmcIdCache = mutableMapOf<Currency, Long?>()

    private val format = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val apiKeyHeader = listOf("X-CMC_PRO_API_KEY", properties.apiKey)
    private val fiatEndpoint = URI("${properties.uri}/fiat/map")
    private val rateEndpoint = URI("${properties.uri}/cryptocurrency/quotes/latest")

    private fun getCurrencyId(currency: Currency): Long? = currencyToCmcIdCache.computeIfAbsent(currency) {
        httpClient.getUrl(requestTimer, fiatEndpoint, additionalHeaders = apiKeyHeader)
            .let { format.decodeFromString<CmcListResponse<FiatCurrency>>(it).data }
            .find { it.symbol == currency.currencyCode }
            ?.id
    }

    override fun getProviderId() = providerId

    override fun isCurrencySupported(currency: Currency) = (getCurrencyId(currency) != null)

    override fun getCurrentRate(
        symbol: TradeSymbol,
        currency: Currency
    ): BigDecimal? {
        val currencyId = getCurrencyId(currency) ?: throw IllegalArgumentException("Unsupported currency '$currency'")
        val uri = symbol.map { URI("$rateEndpoint?symbol=$it&convert_id=$currencyId") }
        return httpClient.getUrl(requestTimer, uri, additionalHeaders = apiKeyHeader)
            .let { format.decodeFromString<CmcMapResponse<CryptoCurrency>>(it).data.values }
            .filter { it.symbol == symbol.value }
            .map { it.quotes["$currencyId"]?.price?.toBigDecimal() }
            .firstOrNull() ?: throw DataExtractionException("Amount not found")
    }
}
