package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
import com.github.spartusch.rateretriever.rate.v1.exception.UnexpectedException
import com.github.spartusch.rateretriever.rate.v1.provider.RateProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Locale

interface RateService {
    fun isRegisteredProviderOrThrow(providerId: String): Boolean
    fun getCurrentRate(providerId: String, symbol: String, currencyCode: String, locale: String): String?
}

private val log = LoggerFactory.getLogger(RateServiceImpl::class.java)

@Service
class RateServiceImpl(
    private val properties: SimpleRateRetrieverProperties,
    private val rateProviders: List<RateProvider>
) : RateService {

    private fun formatRate(rate: BigDecimal, locale: String): String {
        val numberFormat = DecimalFormat.getInstance(Locale.forLanguageTag(locale))
        numberFormat.minimumFractionDigits = properties.fractionDigits
        return numberFormat.format(rate)
    }

    private fun getCurrentRate(provider: RateProvider, symbol: String, currencyCode: String, locale: String): String? {
        require(provider.isCurrencyCodeSupported(currencyCode)) { "Currency code '$currencyCode' is not supported" }
        return try {
            provider
                .getCurrentRate(symbol, currencyCode)
                ?.let { rate -> formatRate(rate, locale) }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            log.error("Error fetching '$symbol' ('$currencyCode'/'$locale') from '${provider.getProviderId()}'", e)
            throw UnexpectedException(e.localizedMessage, e)
        }
    }

    private fun getProviderOrThrow(providerId: String) =
            rateProviders.find { provider -> provider.getProviderId().equals(providerId, ignoreCase = true) }
            ?: throw NotFoundException("No rate provider found for id '$providerId'")

    override fun isRegisteredProviderOrThrow(providerId: String) =
            getProviderOrThrow(providerId).let { true }

    override fun getCurrentRate(providerId: String, symbol: String, currencyCode: String, locale: String) =
            getCurrentRate(getProviderOrThrow(providerId), symbol.toUpperCase(), currencyCode.toUpperCase(), locale)
}
