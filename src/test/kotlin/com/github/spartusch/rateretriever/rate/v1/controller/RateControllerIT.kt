package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TradeSymbol
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
import java.util.Currency
import java.util.Locale

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

    private val providerId = ProviderId("provider")
    private val symbol = TradeSymbol("sym")
    private val currency = Currency.getInstance("EUR")
    private val locale = Locale.forLanguageTag("de-DE")
    private val defaultLocale = Locale.forLanguageTag("en-US")

    @Test
    fun getCurrentRate_happyCase() {
        given(rateService.getCurrentRate(providerId, symbol, currency, locale))
                .willReturn("123,0000")

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getCurrentRate_missingLocaleDefaultsToUs() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
                .willReturn("12.3400")

        mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
                .andExpect(status().isOk)

        verify(rateService, times(1)).getCurrentRate(providerId, symbol, currency, defaultLocale)
    }

    @Test
    fun getCurrentRate_IllegalArgumentException() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
            .willThrow(IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
            .andExpect(status().isBadRequest)
            .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_RuntimeException() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_invalidCurrencyThrows() {
        val result = mockMvc.perform(get("/rate/v1/provider/sym/xxx"))
            .andExpect(status().isBadRequest)
            .andReturn()

        assertThat(result.response.contentAsString).contains("xxx")
    }

    // downloadIqyFileForRequest

    @Test
    fun downloadIqyFileForRequest_happyCase() {
        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_DISPOSITION] = "contentDisposition"
        headers[HttpHeaders.CONTENT_TYPE] = MediaType.APPLICATION_OCTET_STREAM_VALUE
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
                .willReturn(HttpEntity("test".toByteArray(), headers))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "contentDisposition"))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("test")
    }

    @Test
    fun downloadIqyFileForRequest_missingLocaleDefaultsToUs() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)

        mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy"))
                .andExpect(status().isOk)
                .andReturn()

        verify(webQueryService, times(1))
                .getWebQueryEntity("/rate/v1/provider/sym/EUR/?locale=en_US", symbol, currency)
    }

    @Test
    fun downloadIqyFileForRequest_verifiesProviders() {
        mockMvc.perform(get("/rate/v1/fakeProvider/sym/EUR/iqy"))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun downloadIqyFileForRequest_IllegalArgumentException() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }

    @Test
    fun downloadIqyFileForRequest_RuntimeException() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }
}
