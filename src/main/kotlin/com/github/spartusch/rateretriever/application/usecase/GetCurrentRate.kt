package com.github.spartusch.rateretriever.application.usecase

import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.Rate
import com.github.spartusch.rateretriever.domain.model.RateProvider
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.money.CurrencyUnit

private val log = LoggerFactory.getLogger(GetCurrentRate::class.java)

@Service
class GetCurrentRate(rateProviders: List<RateProvider>) {
    private val rateProvidersById = rateProviders.associateBy { it.getProviderId() }

    operator fun invoke(providerId: ProviderId, symbol: TickerSymbol, currency: CurrencyUnit): Rate? {
        val provider = requireNotNull(rateProvidersById[providerId]) {
            "No provider with id '$providerId' found"
        }
        require(provider.isCurrencySupported(currency)) {
            "Provider '$providerId' doesn't support ${currency.currencyCode}"
        }

        return try {
            provider.getCurrentRate(symbol, currency)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching '$symbol' in '$currency' from '${provider.getProviderId()}'", e)
            return null
        }
    }
}
