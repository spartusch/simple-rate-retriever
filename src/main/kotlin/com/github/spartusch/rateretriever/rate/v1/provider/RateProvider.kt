package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import io.micrometer.core.instrument.MeterRegistry
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

interface RateProvider {
    fun getProviderId(): ProviderId

    fun getCurrentRate(
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): MonetaryAmount?

    fun isCurrencySupported(
        currency: CurrencyUnit
    ): Boolean
}

abstract class AbstractTimedRateProvider(private val meterRegistry: MeterRegistry) : RateProvider {
    protected val requestTimer by lazy {
        meterRegistry.timer("provider.requests", "provider.name", getProviderId().toString())
    }
}
