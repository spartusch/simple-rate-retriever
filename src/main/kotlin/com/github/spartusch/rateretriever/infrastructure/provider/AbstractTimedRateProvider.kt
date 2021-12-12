package com.github.spartusch.rateretriever.infrastructure.provider

import com.github.spartusch.rateretriever.domain.model.RateProvider
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer

abstract class AbstractTimedRateProvider(private val meterRegistry: MeterRegistry) : RateProvider {
    protected val requestTimer: Timer by lazy {
        meterRegistry.timer("provider.requests", "provider.name", getProviderId().toString())
    }
}
