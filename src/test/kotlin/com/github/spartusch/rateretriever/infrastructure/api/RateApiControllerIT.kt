package com.github.spartusch.rateretriever.infrastructure.api

import com.github.spartusch.rateretriever.application.configuration.SecurityConfiguration
import com.github.spartusch.rateretriever.application.configuration.SimpleRateRetrieverProperties
import com.github.spartusch.rateretriever.application.usecase.GetCurrentRate
import com.github.spartusch.rateretriever.domain.model.ProviderId
import com.github.spartusch.rateretriever.domain.model.TickerSymbol
import com.github.spartusch.rateretriever.infrastructure.api.generated.RateApiController
import com.github.spartusch.rateretriever.utils.rate
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.NativeWebRequest
import javax.money.Monetary

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [RateApiController::class])
class RateApiControllerIT {
    @Configuration
    @ComponentScan(basePackageClasses = [RateApiController::class, SecurityConfiguration::class])
    class BeanConfiguration {
        @Bean
        fun getCurrentRate() = mockk<GetCurrentRate>()

        @Bean
        fun controllerExceptionHandler() = ControllerExceptionHandler()

        @Bean
        fun rateApiAdapter(
            getCurrentRate: GetCurrentRate,
            nativeWebRequest: NativeWebRequest
        ) = RateApiAdapter(getCurrentRate, nativeWebRequest, SimpleRateRetrieverProperties(4))
    }

    @Autowired
    private lateinit var getCurrentRate: GetCurrentRate

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val base = "http://localhost"
    private val providerId = ProviderId("provider")
    private val symbol = TickerSymbol("sym")
    private val currency = Monetary.getCurrency("EUR")

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    // getCurrentRate

    @Test
    fun getCurrentRate_happyCase() {
        every { getCurrentRate(providerId, symbol, currency) } returns rate("123.0000", currency)

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=de-DE"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString)
                .isEqualTo("123,0000")
    }

    @Test
    fun getCurrentRate_localeIsRequired() {
        every { getCurrentRate(providerId, symbol, currency) } returns rate("12.3400", currency)

        mockMvc.perform(get("/rate/v1/provider/sym/EUR"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun getCurrentRate_IllegalArgumentException() {
        every { getCurrentRate(providerId, symbol, currency) } throws IllegalArgumentException("err_msg")

        val result = mockMvc.perform(get("/rate/v1/provider/sym/EUR?locale=en-US"))
            .andExpect(status().isBadRequest)
            .andReturn()

        assertThat(result.response.contentAsString).contains("err_msg")
    }

    @Test
    fun getCurrentRate_RuntimeException() {
        every { getCurrentRate(providerId, symbol, currency) } throws RuntimeException("err_msg")

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
        val result = mockMvc.perform(get("$base/rate/v1/provider/sym/EUR/iqy?locale=loc"))
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Disposition", "attachment; filename=sym_EUR.iqy"))
                .andExpect(header().string("Content-Type", "text/plain; charset=UTF-8"))
                .andReturn()

        assertThat(result.response.contentAsString).contains("$base/rate/v1/provider/sym/EUR/?locale=loc")
    }

    @Test
    fun downloadIqyFileForRequest_localeIsRequired() {
        mockMvc.perform(get("/rate/v1/provider/sym/EUR/iqy"))
                .andExpect(status().is4xxClientError)
    }
}
