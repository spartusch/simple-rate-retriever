package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.exception.UnexpectedException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Currency
import java.util.Locale

interface RateService {
    fun isRegisteredProviderOrThrow(providerId: ProviderId): Boolean
    fun getCurrentRate(providerId: ProviderId, symbol: TradeSymbol, currency: Currency, locale: Locale): String?
}

private val log = LoggerFactory.getLogger(RateServiceImpl::class.java)

@Service
class RateServiceImpl(
    private val properties: SimpleRateRetrieverProperties,
    private val rateProviders: List<RateProvider>
) : RateService {

    private fun formatRate(
        rate: BigDecimal,
        locale: Locale
    ): String {
        val numberFormat = DecimalFormat.getInstance(locale)
        numberFormat.minimumFractionDigits = properties.fractionDigits
        return numberFormat.format(rate)
    }

    private fun getCurrentRate(
        provider: RateProvider,
        symbol: TradeSymbol,
        currency: Currency,
        locale: Locale
    ): String? {
        require(provider.isCurrencyCodeSupported(currency)) { "Provider doesn't support ${currency.displayName}" }
        return try {
            provider
                .getCurrentRate(symbol, currency)
                ?.let { rate -> formatRate(rate, locale) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching '$symbol' ('$currency'/'$locale') from '${provider.getProviderId()}'", e)
            throw UnexpectedException(e.localizedMessage, e)
        }
    }

    private fun getProviderOrThrow(
        providerId: ProviderId
    ) = rateProviders.find { provider -> provider.getProviderId() == providerId }
        ?: throw IllegalArgumentException("No rate provider found for id '$providerId'")

    override fun isRegisteredProviderOrThrow(
        providerId: ProviderId
    ) = getProviderOrThrow(providerId).let { true }

    override fun getCurrentRate(
        providerId: ProviderId,
        symbol: TradeSymbol,
        currency: Currency,
        locale: Locale
    ) = getCurrentRate(getProviderOrThrow(providerId), symbol, currency, locale)
}
