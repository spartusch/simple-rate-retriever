package com.github.spartusch.rateretriever.infrastructure.api

import com.github.spartusch.rateretriever.application.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.domain.service.RateService
import com.github.spartusch.rateretriever.infrastructure.api.generated.RateApiController
import com.github.spartusch.rateretriever.utils.rate
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
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
import javax.money.Monetary

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [RateApiController::class])
class RateApiControllerIT {
    @Configuration
    @ComponentScan(basePackageClasses = [RateApiController::class])
    class BeanConfiguration {
        @Bean
        fun rateService() = mockk<RateService>()

        @Bean
        fun webQueryService() = mockk<WebQueryService>()

        @Bean
        fun controllerExceptionHandler() = ControllerExceptionHandler()

        @Bean
        fun rateApiAdapter(
            rateService: RateService,
            webQueryService: WebQueryService,
            nativeWebRequest: NativeWebRequest
        ) = RateApiAdapter(rateService, webQueryService, nativeWebRequest, SimpleRateRetrieverProperties(4))
    }

    @Autowired
    private lateinit var rateService: RateService

    @Autowired
    private lateinit var webQueryService: WebQueryService

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val base = "http://localhost"
    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("sym")
    private val currency = Monetary.getCurrency("EUR")

    @Test
    fun getCurrentRate_happyCase() {
        every { rateService.getCurrentRate(providerId, symbol, currency) } returns rate("123.0000", currency)

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getCurrentRate_localeIsRequired() {
        every { rateService.getCurrentRate(providerId, symbol, currency) } returns rate("12.3400", currency)

        mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun getCurrentRate_IllegalArgumentException() {
        every { rateService.getCurrentRate(providerId, symbol, currency) } throws IllegalArgumentException("err_msg")

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=en-US"))
            .andExpect(status().isBadRequest)
            .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_RuntimeException() {
        every { rateService.getCurrentRate(providerId, symbol, currency) } throws java.lang.RuntimeException("err_msg")

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

        every {
            rateService.isRegisteredProviderOrThrow(providerId)
        } returns true

        every {
            webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency)
        } returns HttpEntity(ByteArrayResource("test".toByteArray()), headers)

        val result = mockMvc.perform(get("$base/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "contentDisposition"))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("test")
    }

    @Test
    fun downloadIqyFileForRequest_localeIsRequired() {
        every {
            rateService.isRegisteredProviderOrThrow(providerId)
        } returns true

        every {
            webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=en_US", symbol, currency)
        } returns HttpEntity(ByteArrayResource("test".toByteArray()))

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
        every {
            rateService.isRegisteredProviderOrThrow(providerId)
        } returns true

        every {
            webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency)
        } throws java.lang.IllegalArgumentException("err_msg")

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isBadRequest)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun downloadIqyFileForRequest_RuntimeException() {
        every {
            rateService.isRegisteredProviderOrThrow(providerId)
        } returns true

        every {
            webQueryService.getWebQueryEntity("$base/rate/v1/provider/sym/EUR/?locale=loc", symbol, currency)
        } throws java.lang.RuntimeException("err_msg")

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isInternalServerError)
                .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }
}
