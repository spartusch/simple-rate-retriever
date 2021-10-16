package com.github.spartusch.rateretriever.domain.service

import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.Rate
import com.github.spartusch.rateretriever.domain.model.RateProvider
import com.github.spartusch.rateretriever.domain.model.TickerSymbol

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.money.CurrencyUnit

private val log = LoggerFactory.getLogger(RateService::class.java)

@Service
class RateService(private val rateProviders: List<RateProvider>) {

    private fun getCurrentRate(provider: RateProvider, symbol: TickerSymbol, currency: CurrencyUnit): Rate? {
        require(provider.isCurrencySupported(currency)) { "Provider doesn't support ${currency.currencyCode}" }
        return try {
            provider.getCurrentRate(symbol, currency)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching '$symbol' as '$currency' from '${provider.getProviderId()}'", e)
            return null
        }
    }

    private fun getProviderOrThrow(providerId: ProviderId): RateProvider =
        rateProviders.find { it.getProviderId() == providerId }
        ?: throw IllegalArgumentException("No rate provider found using provider id '$providerId'")

    fun isRegisteredProviderOrThrow(providerId: ProviderId): Boolean =
        getProviderOrThrow(providerId).let { true }

    fun getCurrentRate(providerId: ProviderId, symbol: TickerSymbol, currency: CurrencyUnit): Rate? =
        getCurrentRate(getProviderOrThrow(providerId), symbol, currency)
}
