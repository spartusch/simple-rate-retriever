package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.provider.CoinMarketRateProvider
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import com.github.spartusch.rateretriever.rate.v1.provider.StockExchangeRateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

interface RateService {

    fun getCoinMarketRate(symbol: String, currencyCode: String, locale: String): String?

    fun getStockExchangeRate(symbol: String, currencyCode: String, locale: String): String?

}

private val log = LoggerFactory.getLogger(RateServiceImpl::class.java)

@Service
class RateServiceImpl(private val stockExchangeRateProvider: StockExchangeRateProvider,
                      private val coinMarketRateProvider: CoinMarketRateProvider) : RateService {

    internal fun getCurrentRate(provider: RateProvider, symbol: String, currencyCode: String, locale: String): String? {
        require(provider.isCurrencyCodeSupported(currencyCode)) {
            "Currency code '$currencyCode' is not supported"
        }

        return try {
            provider.getCurrentRate(symbol, currencyCode)?.let {
                val numberFormat = DecimalFormat.getInstance(Locale.forLanguageTag(locale))
                numberFormat.minimumFractionDigits = 4
                numberFormat.format(it)
            }
        } catch (e: Throwable) {
            log.error("Failed to provide rate for '$symbol' ('$currencyCode' / '$locale')", e)
            throw RuntimeException(e.localizedMessage, e)
        }
    }

    override fun getCoinMarketRate(symbol: String, currencyCode: String, locale: String)
            = getCurrentRate(coinMarketRateProvider, symbol, currencyCode, locale)

    override fun getStockExchangeRate(symbol: String, currencyCode: String, locale: String)
            = getCurrentRate(stockExchangeRateProvider, symbol, currencyCode, locale)

}
