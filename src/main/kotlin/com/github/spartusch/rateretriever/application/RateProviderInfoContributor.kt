package com.github.spartusch.rateretriever.application

import com.github.spartusch.rateretriever.domain.model.RateProvider
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class RateProviderInfoContributor(private val rateProviders: List<RateProvider>) : InfoContributor {
    override fun contribute(builder: Info.Builder) {
        builder.withDetail("rate-providers", getRateProviderIds())
    }

    private fun getRateProviderIds() = rateProviders.map {
        it.getProviderId().toString()
    }
}
