package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import com.github.spartusch.webquery.WebQuery
import com.github.spartusch.webquery.WebQueryFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import java.util.Currency

interface WebQueryService {
    fun getWebQueryEntity(
        uri: String,
        symbol: TradeSymbol,
        currency: Currency
    ): HttpEntity<ByteArray>
}

@Service
class WebQueryServiceImpl : WebQueryService {
    override fun getWebQueryEntity(
        uri: String,
        symbol: TradeSymbol,
        currency: Currency
    ): HttpEntity<ByteArray> {
        val webQuery = WebQueryFactory.create(uri)

        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_TYPE] = webQuery.contentType
        headers[HttpHeaders.CONTENT_LENGTH] = webQuery.contentLength.toString()
        headers[HttpHeaders.CONTENT_DISPOSITION] =
            WebQuery.getContentDisposition(symbol.map { sym -> "${sym}_$currency.iqy" })

        return HttpEntity(webQuery.contentBytes, headers)
    }
}
