package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.exception.UnexpectedException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

interface RateService {
    fun isRegisteredProviderOrThrow(providerId: ProviderId): Boolean
    fun getCurrentRate(providerId: ProviderId, symbol: TickerSymbol, currency: CurrencyUnit, locale: Locale): String?
}

private val log = LoggerFactory.getLogger(RateServiceImpl::class.java)

@Service
class RateServiceImpl(
    private val properties: SimpleRateRetrieverProperties,
    private val rateProviders: List<RateProvider>
) : RateService {

    private fun formatRate(rate: MonetaryAmount, locale: Locale): String {
        val numberFormat = DecimalFormat.getInstance(locale)
        numberFormat.minimumFractionDigits = properties.fractionDigits
        return numberFormat.format(rate.number)
    }

    private fun getCurrentRate(
        provider: RateProvider,
        symbol: TickerSymbol,
        currency: CurrencyUnit,
        locale: Locale
    ): String? {
        require(provider.isCurrencySupported(currency)) { "Provider doesn't support ${currency.currencyCode}" }
        return try {
            provider.getCurrentRate(symbol, currency)?.let { formatRate(it, locale) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching $symbol ('$currency'/'$locale') from ${provider.getProviderId()}", e)
            throw UnexpectedException(e.message ?: "No error message provided", e)
        }
    }

    private fun getProviderOrThrow(providerId: ProviderId) =
        rateProviders.find { it.getProviderId() == providerId }
        ?: throw IllegalArgumentException("No rate provider found for id '$providerId'")

    override fun isRegisteredProviderOrThrow(providerId: ProviderId) =
        getProviderOrThrow(providerId).let { true }

    override fun getCurrentRate(providerId: ProviderId, symbol: TickerSymbol, currency: CurrencyUnit, locale: Locale) =
        getCurrentRate(getProviderOrThrow(providerId), symbol, currency, locale)
}
