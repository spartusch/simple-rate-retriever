package com.github.spartusch.rateretriever.infrastructure.api

import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.webquery.WebQueryFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource
import javax.money.Monetary

class WebQueryExtensionsTest {

    private val symbol = TickerSymbol("symbol")
    private val currency = Monetary.getCurrency("EUR")

    private val cut = WebQueryFactory.create("someUri")

    @Test
    fun toHttpEntity_content() {
        val entity = cut.toHttpEntity(symbol, currency)
        assertThat(entity.body).isEqualTo(ByteArrayResource(cut.contentBytes))
    }

    @Test
    fun toHttpEntity_contentType() {
        val entity = cut.toHttpEntity(symbol, currency)
        assertThat(entity.headers).contains(entry("Content-Type", listOf("text/plain; charset=UTF-8")))
    }

    @Test
    fun toHttpEntity_contentLength() {
        val entity = cut.toHttpEntity(symbol, currency)
        assertThat(entity.headers).contains(entry("Content-Length", listOf(cut.contentLength.toString())))
    }

    @Test
    fun toHttpEntity_contentDisposition() {
        val entity = cut.toHttpEntity(symbol, currency)
        assertThat(entity.headers)
            .contains(entry("Content-Disposition", listOf("attachment; filename=symbol_EUR.iqy")))
    }
}
