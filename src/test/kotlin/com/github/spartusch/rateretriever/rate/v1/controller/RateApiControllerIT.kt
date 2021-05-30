package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import com.github.spartusch.rateretriever.rate.v1.controller.generated.RateApiController
import com.github.spartusch.rateretriever.rate.v1.model.ProviderId
import com.github.spartusch.rateretriever.rate.v1.model.TickerSymbol
import com.github.spartusch.rateretriever.rate.v1.service.RateService
import com.github.spartusch.rateretriever.rate.v1.service.WebQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.request.NativeWebRequest
import java.util.Locale
import javax.money.Monetary

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [RateApiController::class])
class RateApiControllerIT {

    @MockBean
    private lateinit var rateService: RateService

    @MockBean
    private lateinit var webQueryService: WebQueryService

    @MockBean
    @Suppress("UnusedPrivateMember")
    private lateinit var requestLoggingFilterProperties: RequestLoggingFilterProperties

    @TestConfiguration
    class BeanConfiguration {
        @Bean
        fun rateApiAdapter(
            rateService: RateService,
            webQueryService: WebQueryService,
            nativeWebRequest: NativeWebRequest
        ) = RateApiAdapter(rateService, webQueryService, nativeWebRequest)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val base = "http://localhost"
    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("sym")
    private val currency = Monetary.getCurrency("EUR")
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
    fun getCurrentRate_localeIsRequired() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
            .willReturn("12.3400")

        mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun getCurrentRate_IllegalArgumentException() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
            .willThrow(IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=en-US"))
            .andExpect(status().isBadRequest)
            .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_RuntimeException() {
        given(rateService.getCurrentRate(providerId, symbol, currency, defaultLocale))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=en-US"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_invalidCurrencyThrows() {
        val result = mockMvc.perform(get("/rate/v1/provider/sym/xxx?locale=en-US"))
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
        `when`(webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
            .thenReturn(HttpEntity(ByteArrayResource("test".toByteArray()), headers))

        val result = mockMvc.perform(get("$base/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "contentDisposition"))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("test")
    }

    @Test
    fun downloadIqyFileForRequest_localeIsRequired() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        `when`(webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=en_US", symbol, currency))
            .thenReturn(HttpEntity(ByteArrayResource("test".toByteArray())))

        mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy"))
                .andExpect(status().is4xxClientError)
    }

    @Test
    fun downloadIqyFileForRequest_verifiesProviders() {
        mockMvc.perform(get("/rate/v1/fakeProvider/sym/EUR/iqy"))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun downloadIqyFileForRequest_IllegalArgumentException() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        given(webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun downloadIqyFileForRequest_RuntimeException() {
        given(rateService.isRegisteredProviderOrThrow(providerId)).willReturn(true)
        given(webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }
}
