package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import com.github.spartusch.rateretriever.rate.v1.service.RateService
import com.github.spartusch.rateretriever.rate.v1.service.WebQueryService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.Currency
import java.util.Locale
import javax.servlet.http.HttpServletRequest

private const val DEFAULT_LOCALE = "en-US"

@RestController
@RequestMapping("/rate/v1")
class RateController(private val rateService: RateService, private val webQueryService: WebQueryService) {

    @RequestMapping("/{providerId}/{symbol}/{currency}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCurrentRate(
        @PathVariable("providerId") providerId: ProviderId,
        @PathVariable("symbol") symbol: TradeSymbol,
        @PathVariable("currency") currency: Currency,
        @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: Locale
    ) = rateService.getCurrentRate(providerId, symbol, currency, locale)

    @RequestMapping("/{providerId}/{symbol}/{currency}/iqy", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadIqyFileForRequest(
        @PathVariable("providerId") providerId: ProviderId,
        @PathVariable("symbol") symbol: TradeSymbol,
        @PathVariable("currency") currency: Currency,
        @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: Locale,
        request: HttpServletRequest
    ): HttpEntity<ByteArray> {
        require(rateService.isRegisteredProviderOrThrow(providerId))
        return removeIqyIndicator(request.requestURI, locale)
            .let { rateEndpoint -> webQueryService.getWebQueryEntity(rateEndpoint, symbol, currency) }
    }

    // Remove "/iqy" suffix from the request URI so the web query will call the actual rate endpoint
    private fun removeIqyIndicator(
        requestUri: String,
        locale: Locale
    ) = requestUri.replaceAfterLast('/', "?locale=$locale")

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleException(
        e: NotFoundException
    ): String? = e.localizedMessage

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(
        e: MethodArgumentTypeMismatchException
    ): String? = "'${e.value}' is not a valid ${e.name}"

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(
        e: IllegalArgumentException
    ): String? = e.localizedMessage

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(
        e: RuntimeException
    ): String? = e.localizedMessage
}
