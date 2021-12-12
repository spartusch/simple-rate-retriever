package com.github.spartusch.rateretriever.infrastructure.api

import com.github.spartusch.rateretriever.application.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.application.usecase.GetCurrentRate
import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.infrastructure.api.generated.RateApiDelegate
import com.github.spartusch.webquery.WebQueryFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.context.request.NativeWebRequest
import java.util.Locale
import java.util.Optional
import java.util.function.Supplier
import javax.money.Monetary
import javax.servlet.http.HttpServletRequest

@Service
class RateApiAdapter(
    private val getCurrentRate: GetCurrentRate,
    private val nativeWebRequest: NativeWebRequest,
    private val properties: SimpleRateRetrieverProperties
) : RateApiDelegate {

    override fun getRequest() = Optional.ofNullable(nativeWebRequest)

    @Suppress("TooGenericExceptionCaught")
    private fun <T> mapExceptions(messageFormat: String, vararg messageParams: Any, supplier: Supplier<T>) =
        try {
            supplier.get()
        } catch (e: RuntimeException) {
            throw IllegalArgumentException(String.format(messageFormat, *messageParams), e)
        }

    private fun parseCurrencyCode(code: String) =
        mapExceptions("'%s' is not a valid currency", code) {
            Monetary.getCurrency(code)
        }

    private fun parseLocale(locale: String) =
        mapExceptions("'%s' is not a valid locale", locale) {
            Locale.forLanguageTag(locale.replace('_', '-'))
        }

    override fun getCurrentRate(
        providerId: String,
        tickerSymbol: String,
        currencyCode: String,
        locale: String
    ): ResponseEntity<String> {
        val rate = getCurrentRate(
            ProviderId(providerId),
            TickerSymbol(tickerSymbol),
            parseCurrencyCode(currencyCode)
        )

        return if (rate != null) {
            ResponseEntity.ok(rate.format(parseLocale(locale), properties.fractionDigits))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    override fun downloadWebQueryForRequest(
        providerId: String,
        tickerSymbol: String,
        currencyCode: String,
        locale: String
    ): ResponseEntity<Resource> {
        val request = nativeWebRequest.nativeRequest as HttpServletRequest
        val endpoint = removeIqyIndicator(request.requestURL.toString(), parseLocale(locale))
        val entity = WebQueryFactory.create(endpoint)
            .toHttpEntity(TickerSymbol(tickerSymbol), parseCurrencyCode(currencyCode))

        return if (entity.hasBody()) {
            ResponseEntity(entity.body, entity.headers, HttpStatus.OK)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Remove "/iqy" suffix from the request URI so the web query will call the actual rate endpoint
    private fun removeIqyIndicator(requestUrl: String, locale: Locale) =
        requestUrl.replaceAfterLast('/', "?locale=${locale.toLanguageTag()}")
}
