package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.webquery.WebQuery
import com.github.spartusch.webquery.WebQueryFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

interface WebQueryService {
    fun getWebQueryEntity(uri: String, symbol: String, currencyCode: String): HttpEntity<ByteArray>
}

@Service
class WebQueryServiceImpl : WebQueryService {
    override fun getWebQueryEntity(uri: String, symbol: String, currencyCode: String): HttpEntity<ByteArray> {
        val webQuery = WebQueryFactory.create(uri)

        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_TYPE] = webQuery.contentType
        headers[HttpHeaders.CONTENT_LENGTH] = webQuery.contentLength.toString()
        headers[HttpHeaders.CONTENT_DISPOSITION] =
            WebQuery.getContentDisposition("${symbol}_${currencyCode.toUpperCase()}.iqy")

        return HttpEntity(webQuery.contentBytes, headers)
    }
}
