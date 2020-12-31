package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.spartusch.webquery.WebQuery
import com.github.spartusch.webquery.WebQueryFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import java.util.Currency

interface WebQueryService {
    fun getWebQueryEntity(
        uri: String,
        symbol: TickerSymbol,
        currency: Currency
    ): HttpEntity<ByteArrayResource>
}

@Service
class WebQueryServiceImpl : WebQueryService {
    override fun getWebQueryEntity(
        uri: String,
        symbol: TickerSymbol,
        currency: Currency
    ): HttpEntity<ByteArrayResource> {
        val webQuery = WebQueryFactory.create(uri)

        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_TYPE] = webQuery.contentType
        headers[HttpHeaders.CONTENT_LENGTH] = webQuery.contentLength.toString()
        headers[HttpHeaders.CONTENT_DISPOSITION] =
            WebQuery.getContentDisposition(symbol.map { sym -> "${sym}_$currency.iqy" })

        val resource = ByteArrayResource(webQuery.contentBytes)

        return HttpEntity(resource, headers)
    }
}
