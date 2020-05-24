package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CoinMarketCapRateProvider(private val properties: CoinMarketCapProperties) : CoinMarketRateProvider {

    private val supportedCurrencies = listOf("AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK",
            "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD",
            "PHP", "PKR", "PLN", "RUB", "SEK", "SGD", "THB", "TRY", "TWD", "ZAR", "USD")

    override fun isCurrencyCodeSupported(currencyCode: String) = supportedCurrencies.contains(currencyCode.toUpperCase())

    override fun getCurrentRate(symbol: String, currencyCode: String): BigDecimal? {
        TODO("Not yet implemented")
    }

}
