package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.configuration.CoinMarketCapProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Currency

@Service
class CoinMarketCapRateProvider(private val properties: CoinMarketCapProperties) : RateProvider {

    private val providerId = ProviderId(properties.id)

    private val supportedCurrencies = listOf("AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK",
            "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD",
            "PHP", "PKR", "PLN", "RUB", "SEK", "SGD", "THB", "TRY", "TWD", "ZAR", "USD")

    override fun getProviderId() = providerId

    override fun isCurrencyCodeSupported(
        currency: Currency
    ) = supportedCurrencies.contains(currency.currencyCode) // Upper case?

    override fun getCurrentRate(
        symbol: TradeSymbol,
        currency: Currency
    ): BigDecimal? {
        TODO("Not yet implemented")
    }
}
