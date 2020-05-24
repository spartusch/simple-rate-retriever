package com.github.spartusch.rateretriever.rate.v1.controller

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

    @RequestMapping("/coinmarket/{symbol}/{currency}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getCoinMarketRate(@PathVariable("symbol") symbol: String,
                          @PathVariable("currency") currencyCode: String,
                          @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: String)
            = rateService.getCoinMarketRate(symbol, currencyCode, locale)

    @RequestMapping("/stockexchange/{symbol}/{currency}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getStockExchangeRate(@PathVariable("symbol") symbol: String,
                             @PathVariable("currency") currencyCode: String,
                             @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: String)
            = rateService.getStockExchangeRate(symbol, currencyCode, locale)

    @RequestMapping("/{provider}/{symbol}/{currency}/iqy", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadIqyFileForRequest(@PathVariable("provider") provider: String,
                                  @PathVariable("symbol") symbol: String,
                                  @PathVariable("currency") currencyCode: String,
                                  @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) locale: String,
                                  request: HttpServletRequest): HttpEntity<ByteArray> {
        require(provider == "coinmarket" || provider == "stockexchange")
        val targetUri = request.requestURI.toString().replaceAfterLast('/', "?locale=$locale")
            // Remove "/iqy" suffix from the request URI so the web query calls the actual rate endpoint
        return webQueryService.getWebQueryEntity(targetUri, symbol, currencyCode)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(e: IllegalArgumentException): String? = e.localizedMessage

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(e: RuntimeException): String? = e.localizedMessage

}
