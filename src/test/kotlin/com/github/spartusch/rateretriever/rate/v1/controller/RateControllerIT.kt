package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import com.github.spartusch.rateretriever.rate.v1.service.RateService
import com.github.spartusch.rateretriever.rate.v1.service.WebQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [RateController::class])
class RateControllerIT {

    @MockBean
    private lateinit var rateService: RateService

    @MockBean
    private lateinit var webQueryService: WebQueryService

    @MockBean
    private lateinit var requestLoggingFilterProperties: RequestLoggingFilterProperties

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun getStockExchangeRate_happyCase() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "de-DE"))
                .willReturn("123,0000")

        val result = mockMvc.perform(get("/rate/v1/stockexchange/ETF110/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getStockExchangeRate_missingLocaleDefaultsToUs() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "en-US"))
                .willReturn("12.3400")

        mockMvc.perform(get("/rate/v1/stockexchange/ETF110/EUR"))
                .andExpect(status().isOk)

        verify(rateService, times(1)).getStockExchangeRate("ETF110", "EUR", "en-US")
    }

    @Test
    fun getStockExchangeRate_IllegalArgumentException() {
        given(rateService.getStockExchangeRate("xxx", "yyy", "en-US"))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/stockexchange/xxx/yyy"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getStockExchangeRate_RuntimeException() {
        given(rateService.getStockExchangeRate("xxx", "yyy", "en-US"))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/stockexchange/xxx/yyy"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    // getCoinMarketRate

    @Test
    fun getCoinMarketRate_happyCase() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "de-DE"))
                .willReturn("123,0000")

        val result = mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getCoinMarketRate_missingLocaleDefaultsToUs() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "en-US"))
                .willReturn("12.3400")

        mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/EUR"))
                .andExpect(status().isOk)

        verify(rateService, times(1)).getCoinMarketRate("bitcoin", "EUR", "en-US")
    }

    @Test
    fun getCoinMarketRate_IllegalArgumentException() {
        given(rateService.getCoinMarketRate("xxx", "yyy", "en-US"))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/coinmarket/xxx/yyy"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCoinMarketRate_RuntimeException() {
        given(rateService.getCoinMarketRate("xxx", "yyy", "en-US"))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/coinmarket/xxx/yyy"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    // downloadIqyFileForRequest

    @Test
    fun downloadIqyFileForRequest_happyCase() {
        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_DISPOSITION] = "contentDisposition"
        headers[HttpHeaders.CONTENT_TYPE] = MediaType.APPLICATION_OCTET_STREAM_VALUE
        given(webQueryService.getWebQueryEntity("/rate/v1/stockexchange/symbol/currency/?locale=loc", "symbol", "currency"))
                .willReturn(HttpEntity("test".toByteArray(), headers))

        val result = mockMvc.perform(get("/rate/v1/stockexchange/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "contentDisposition"))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("test")
    }

    @Test
    fun downloadIqyFileForRequest_missingLocaleDefaultsToUs() {
        given(webQueryService.getWebQueryEntity("/rate/v1/stockexchange/symbol/currency/?locale=en-US", "symbol", "currency"))
                .willReturn(HttpEntity("test".toByteArray(), HttpHeaders()))

        mockMvc.perform(get("/rate/v1/stockexchange/symbol/currency/iqy"))
                .andExpect(status().isOk)
                .andReturn()

        verify(webQueryService, times(1))
                .getWebQueryEntity("/rate/v1/stockexchange/symbol/currency/?locale=en-US", "symbol", "currency")
    }

    @Test
    fun downloadIqyFileForRequest_verifiesProviders() {
        mockMvc.perform(get("/rate/v1/fakeProvider/symbol/currency/iqy"))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun downloadIqyFileForRequest_IllegalArgumentException() {
        given(webQueryService.getWebQueryEntity("/rate/v1/stockexchange/symbol/currency/?locale=loc", "symbol", "currency"))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/stockexchange/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }

    @Test
    fun downloadIqyFileForRequest_RuntimeException() {
        given(webQueryService.getWebQueryEntity("/rate/v1/stockexchange/symbol/currency/?locale=loc", "symbol", "currency"))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/stockexchange/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }

}
