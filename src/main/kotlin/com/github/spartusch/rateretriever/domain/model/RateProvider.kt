package com.github.spartusch.rateretriever.domain.model

import javax.money.CurrencyUnit

interface RateProvider {
    fun getProviderId(): ProviderId

    fun getCurrentRate(
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): Rate?

    fun isCurrencySupported(
        currency: CurrencyUnit
    ): Boolean
}
