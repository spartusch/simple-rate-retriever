package com.github.spartusch.rateretriever.infrastructure.api

import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.webquery.WebQuery
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import javax.money.CurrencyUnit

fun WebQuery.toHttpEntity(
    symbol: TickerSymbol,
    currency: CurrencyUnit
): HttpEntity<ByteArrayResource> {
    val fileName = "${symbol}_${currency.currencyCode}.iqy"

    val headers = HttpHeaders()
    headers[HttpHeaders.CONTENT_TYPE] = this.contentType
    headers[HttpHeaders.CONTENT_LENGTH] = this.contentLength.toString()
    headers[HttpHeaders.CONTENT_DISPOSITION] = WebQuery.getContentDisposition(fileName)

    val resource = ByteArrayResource(this.contentBytes)

    return HttpEntity(resource, headers)
}
