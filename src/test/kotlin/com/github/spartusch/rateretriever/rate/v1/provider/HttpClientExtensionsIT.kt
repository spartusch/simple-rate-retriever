package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.jenspiegsa.wiremockextension.InjectServer
import com.github.jenspiegsa.wiremockextension.WireMockExtension
import com.github.spartusch.rateretriever.rate.WireMockUtils.stubResponse
import com.github.spartusch.rateretriever.rate.v1.exception.RequestException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import java.net.http.HttpClient

@ExtendWith(WireMockExtension::class)
class HttpClientExtensionsIT {

    @InjectServer
    private lateinit var serverMock: WireMockServer

    private val timer = SimpleMeterRegistry().timer("testTimer")

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
    fun getUrl_returnsContentForSuccessfulRequests(
        statusCode: Int
    ) {
        stubResponse(uriPath, "test response", statusCode)
        val response = cut.getUrl(timer, uri, "")
        assertThat(response).isEqualTo("test response")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 410, 500, 503])
    fun getUrl_throwsExceptionIfGettingErrorStatusCode(
        statusCode: Int
    ) {
        stubResponse(uriPath, "", statusCode)
        val e = ThrowableAssert.catchThrowableOfType({ cut.getUrl(timer, uri, "") }, RequestException::class.java)
        assertThat(e).hasMessageContaining(uriPath)
    }

    @Test
    fun getUrl_setsDefaultHeaders() {
        stubResponse(uriPath, "", 200)

        cut.getUrl(timer, uri)

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(uriPath))
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("User-Agent", WireMock.matching(".+")))
    }

    @Test
    fun getUrl_setsCustomAcceptHeader() {
        stubResponse(uriPath, "", 200)

        cut.getUrl(timer, uri, "some/accept")

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(uriPath))
                .withHeader("Accept", WireMock.equalTo("some/accept")))
    }

    @Test
    fun getUrl_setsAdditionalHeaders() {
        stubResponse(uriPath, "", 200)

        cut.getUrl(timer, uri, additionalHeaders = listOf("A", "a", "B", "b"))

        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo(uriPath))
            .withHeader("A", WireMock.equalTo("a"))
            .withHeader("B", WireMock.equalTo("b")))
    }
}
