package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
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
import javax.servlet.http.HttpServletRequest

private const val DEFAULT_LOCALE = "en-US"

@RestController
@RequestMapping("/rate/v1")
class RateController(private val rateService: RateService, private val webQueryService: WebQueryService) {

    @RequestMapping("/{providerId}/{symbol}/{currency}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCurrentRate(
        @PathVariable("providerId") providerId: String,
        @PathVariable("symbol") symbol: String,
        @PathVariable("currency") currencyCode: String,
        @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: String
    ) = rateService.getCurrentRate(providerId, symbol, currencyCode, locale)

    @RequestMapping("/{providerId}/{symbol}/{currency}/iqy", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadIqyFileForRequest(
        @PathVariable("providerId") providerId: String,
        @PathVariable("symbol") symbol: String,
        @PathVariable("currency") currencyCode: String,
        @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: String,
        request: HttpServletRequest
    ): HttpEntity<ByteArray> {
        require(rateService.isRegisteredProviderOrThrow(providerId))
        return removeIqyIndicator(request.requestURI.toString(), locale)
            .let { rateEndpoint -> webQueryService.getWebQueryEntity(rateEndpoint, symbol, currencyCode) }
    }

    // Remove "/iqy" suffix from the request URI so the web query will call the actual rate endpoint
    private fun removeIqyIndicator(requestUri: String, locale: String) =
        requestUri.replaceAfterLast('/', "?locale=$locale")

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleException(e: NotFoundException): String? = e.localizedMessage

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(e: IllegalArgumentException): String? = e.localizedMessage

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(e: RuntimeException): String? = e.localizedMessage
}
