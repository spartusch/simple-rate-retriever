package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import java.math.BigDecimal
import java.util.Currency

interface RateProvider {
    fun getProviderId(): ProviderId

    fun getCurrentRate(
        symbol: TradeSymbol,
        currency: Currency
    ): BigDecimal?

    fun isCurrencyCodeSupported(
        currency: Currency
    ): Boolean
}
