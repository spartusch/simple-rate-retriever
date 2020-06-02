package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock
import com.github.jenspiegsa.wiremockextension.InjectServer
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import com.github.spartusch.rateretriever.rate.WireMockUtils.stubResponse
import com.github.spartusch.rateretriever.rate.v1.exception.RequestException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micrometer.core.instrument.Timer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import java.net.http.HttpClient
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@ExtendWith(WireMockExtension::class)
class HttpClientExtensionsIT {

    @InjectServer
    private lateinit var serverMock: WireMockServer

    @ConfigureWireMock
    private val options = WireMockConfiguration.wireMockConfig().dynamicPort()

    private val timer = object : Timer {
        var recordCalled = false

        override fun <T : Any?> recordCallable(f: Callable<T>): T { recordCalled = true; return f.call() }
        override fun <T : Any?> record(f: Supplier<T>): T { recordCalled = true; return f.get() }
        override fun record(f: Runnable) { recordCalled = true; f.run() }

        override fun record(amount: Long, unit: TimeUnit) = TODO()
        override fun takeSnapshot() = TODO()
        override fun getId() = TODO()
        override fun max(unit: TimeUnit) = TODO()
        override fun count() = TODO()
        override fun totalTime(unit: TimeUnit) = TODO()
        override fun baseTimeUnit() = TODO()
    }

    private lateinit var uri: URI
    private lateinit var uriPath: String

    private lateinit var cut: HttpClient

    @BeforeEach
    fun setUp() {
        uriPath = "/api/header/search?q="
        uri = URI("${serverMock.baseUrl()}$uriPath")
        cut = HttpClient.newHttpClient()
    }

    @ParameterizedTest
    @ValueSource(ints = [200, 201, 301, 302, 399])
    fun getUrl_returnsContentForSuccessfulRequests(statusCode: Int) {
        stubResponse(uriPath, "test response", statusCode)
        val response = cut.getUrl(uri, "", timer)
        assertThat(response).isEqualTo("test response")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 410, 500, 503])
    fun getUrl_throwsExceptionIfGettingErrorStatusCode(statusCode: Int) {
        stubResponse(uriPath, "", statusCode)
        val e = ThrowableAssert.catchThrowableOfType({ cut.getUrl(uri, "", timer) }, RequestException::class.java)
        assertThat(e).hasMessageContaining(uriPath)
    }

    @Test
    fun getUrl_setsExpectedHeaders() {
        stubResponse(uriPath, "", 200)

        cut.getUrl(uri, "some/accept", timer)

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(uriPath))
                .withHeader("Accept", WireMock.equalTo("some/accept"))
                .withHeader("User-Agent", WireMock.matching(".+")))
    }

    @Test
    fun getUrl_callsRequestTimer() {
        stubResponse(uriPath, "", 200)
        cut.getUrl(uri, "", timer)
        assertThat(timer.recordCalled).isTrue()
    }
}
