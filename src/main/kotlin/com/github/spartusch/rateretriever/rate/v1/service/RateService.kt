package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
import com.github.spartusch.rateretriever.rate.v1.exception.UnexpectedException
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale

interface RateService {
    fun isRegisteredProviderOrThrow(providerId: String): Boolean
    fun getCurrentRate(providerId: String, symbol: String, currencyCode: String, locale: String): String?
}

private val log = LoggerFactory.getLogger(RateServiceImpl::class.java)

@Service
class RateServiceImpl(private val stockExchangeRateProvider: StockExchangeRateProvider,
                      private val coinMarketRateProvider: CoinMarketRateProvider) : RateService {

    private fun formatRate(rate: BigDecimal, locale: String): String {
        val numberFormat = DecimalFormat.getInstance(Locale.forLanguageTag(locale))
        numberFormat.minimumFractionDigits = properties.fractionDigits
        return numberFormat.format(rate)
    }

    private fun getCurrentRate(provider: RateProvider, symbol: String, currencyCode: String, locale: String): String? {
        require(provider.isCurrencyCodeSupported(currencyCode)) { "Currency code '$currencyCode' is not supported" }
        return try {
            provider
                .getCurrentRate(symbol, currencyCode)
                ?.let { rate -> formatRate(rate, locale) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching '$symbol' ('$currencyCode'/'$locale') from '${provider.getProviderId()}'", e)
            throw UnexpectedException(e.localizedMessage, e)
        }
    }

    override fun getCoinMarketRate(symbol: String, currencyCode: String, locale: String)
            = getCurrentRate(coinMarketRateProvider, symbol, currencyCode, locale)

    override fun getStockExchangeRate(symbol: String, currencyCode: String, locale: String)
            = getCurrentRate(stockExchangeRateProvider, symbol, currencyCode, locale)

}
