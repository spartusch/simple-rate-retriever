package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import io.micrometer.core.instrument.MeterRegistry
import java.math.BigDecimal
import java.util.Currency

interface RateProvider {
    fun getProviderId(): ProviderId

    fun getCurrentRate(
        symbol: TickerSymbol,
        currency: Currency
    ): BigDecimal?

    fun isCurrencySupported(
        currency: Currency
    ): Boolean
}

abstract class AbstractTimedRateProvider(private val meterRegistry: MeterRegistry) : RateProvider {
    protected val requestTimer by lazy {
        meterRegistry.timer("provider.requests", "provider.name", getProviderId().value)
    }
}
