package com.github.spartusch.rateretriever.rate.v1.service

import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
import com.github.spartusch.webquery.WebQueryFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Currency

class WebQueryServiceImplTest {

    private lateinit var cut: WebQueryServiceImpl

    private val symbol = TradeSymbol("symbol")
    private val currency = Currency.getInstance("EUR")

    @BeforeEach
    fun setUp() {
        cut = WebQueryServiceImpl()
    }

    @Test
    fun getWebQueryEntity_content() {
        val webQuery = WebQueryFactory.create("someUri")
        val entity = cut.getWebQueryEntity("someUri", symbol, currency)
        assertThat(entity.body).isEqualTo(webQuery.contentBytes)
    }

    @Test
    fun getWebQueryEntity_contentType() {
        val entity = cut.getWebQueryEntity("someUri", symbol, currency)
        assertThat(entity.headers).contains(entry("Content-Type", listOf("text/plain; charset=UTF-8")))
    }

    @Test
    fun getWebQueryEntity_contentLength() {
        val webQuery = WebQueryFactory.create("someUri")
        val entity = cut.getWebQueryEntity("someUri", symbol, currency)
        assertThat(entity.headers).contains(entry("Content-Length", listOf(webQuery.contentLength.toString())))
    }

    @Test
    fun getWebQueryEntity_contentDisposition() {
        val entity = cut.getWebQueryEntity("someUri", symbol, currency)
        assertThat(entity.headers)
            .contains(entry("Content-Disposition", listOf("attachment; filename=symbol_EUR.iqy")))
    }
}
