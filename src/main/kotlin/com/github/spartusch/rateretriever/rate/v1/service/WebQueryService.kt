package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.spartusch.webquery.WebQuery
import com.github.spartusch.webquery.WebQueryFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import javax.money.CurrencyUnit

interface WebQueryService {
    fun getWebQueryEntity(
        uri: String,
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): HttpEntity<ByteArrayResource>
}

@Service
class WebQueryServiceImpl : WebQueryService {
    override fun getWebQueryEntity(
        uri: String,
        symbol: TickerSymbol,
        currency: CurrencyUnit
    ): HttpEntity<ByteArrayResource> {
        val webQuery = WebQueryFactory.create(uri)
        val fileName = "${symbol}_${currency.currencyCode}.iqy"

        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_TYPE] = webQuery.contentType
        headers[HttpHeaders.CONTENT_LENGTH] = webQuery.contentLength.toString()
        headers[HttpHeaders.CONTENT_DISPOSITION] = WebQuery.getContentDisposition(fileName)

        val resource = ByteArrayResource(webQuery.contentBytes)

        return HttpEntity(resource, headers)
    }
}
