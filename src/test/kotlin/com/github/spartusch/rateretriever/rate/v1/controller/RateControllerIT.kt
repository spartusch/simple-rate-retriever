package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import com.github.spartusch.rateretriever.rate.v1.exception.NotFoundException
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
    fun getCurrentRate_happyCase() {
        given(rateService.getCurrentRate("provider", "ETF110", "EUR", "de-DE"))
                .willReturn("123,0000")

        val result = mockMvc.perform(get("/rate/v1/provider/ETF110/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getCurrentRate_missingLocaleDefaultsToUs() {
        given(rateService.getCurrentRate("provider", "ETF110", "EUR", "en-US"))
                .willReturn("12.3400")

        mockMvc.perform(get("/rate/v1/provider/ETF110/EUR"))
                .andExpect(status().isOk)

        verify(rateService, times(1)).getCurrentRate("provider", "ETF110", "EUR", "en-US")
    }

    @Test
    fun getCurrentRate_IllegalArgumentException() {
        given(rateService.getCurrentRate("provider", "xxx", "yyy", "en-US"))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/xxx/yyy"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_NotFoundException() {
        given(rateService.getCurrentRate("provider", "xxx", "yyy", "en-US"))
            .willThrow(NotFoundException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/xxx/yyy"))
            .andExpect(status().isNotFound)
            .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_RuntimeException() {
        given(rateService.getCurrentRate("provider", "xxx", "yyy", "en-US"))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/xxx/yyy"))
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
        given(rateService.isRegisteredProviderOrThrow("provider")).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/currency/?locale=loc", "sym", "currency"))
                .willReturn(HttpEntity("test".toByteArray(), headers))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/currency/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "contentDisposition"))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("test")
    }

    @Test
    fun downloadIqyFileForRequest_missingLocaleDefaultsToUs() {
        given(rateService.isRegisteredProviderOrThrow("provider")).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/currency/?locale=en-US", "sym", "currency"))
                .willReturn(HttpEntity("test".toByteArray(), HttpHeaders()))

        mockMvc.perform(get("/rate/v1/provider/sym/currency/iqy"))
                .andExpect(status().isOk)
                .andReturn()

        verify(webQueryService, times(1))
                .getWebQueryEntity("/rate/v1/provider/sym/currency/?locale=en-US", "sym", "currency")
    }

    @Test
    fun downloadIqyFileForRequest_verifiesProviders() {
        mockMvc.perform(get("/rate/v1/fakeProvider/sym/currency/iqy"))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun downloadIqyFileForRequest_IllegalArgumentException() {
        given(rateService.isRegisteredProviderOrThrow("provider")).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/currency/?locale=loc", "sym", "currency"))
                .willThrow(java.lang.IllegalArgumentException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/currency/iqy?locale=loc"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }

    @Test
    fun downloadIqyFileForRequest_RuntimeException() {
        given(rateService.isRegisteredProviderOrThrow("provider")).willReturn(true)
        given(webQueryService.getWebQueryEntity("/rate/v1/provider/sym/currency/?locale=loc", "sym", "currency"))
                .willThrow(java.lang.RuntimeException("err_msg"))

        val result = mockMvc.perform(get("/rate/v1/provider/sym/currency/iqy?locale=loc"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("err_msg")
    }
}
