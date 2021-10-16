package com.github.spartusch.rateretriever.infrastructure.provider.extensions

import com.github.spartusch.rateretriever.infrastructure.provider.exception.RequestException
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val USER_AGENT = "User Agent"

private val log = LoggerFactory.getLogger("RateProviderExtensionsKt")

@Suppress("MagicNumber")
private val validStatusCodeRange = 1..399

fun HttpClient.getUrl(
    requestTimer: Timer,
    uri: URI,
    accept: String = MediaType.APPLICATION_JSON_VALUE,
    additionalHeaders: List<String> = listOf()
): String {
    val headers = mutableListOf(
        "User-Agent", USER_AGENT,
        "Accept", accept
    )
    headers.addAll(additionalHeaders)

    @Suppress("SpreadOperator")
    val request = HttpRequest.newBuilder(uri)
        .headers(*headers.toTypedArray())
        .build()

    log.debug("Fetching url '{}'", uri)

    val body = requestTimer.recordCallable {
        val response = this.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in validStatusCodeRange) {
            log.error("Failed to fetch '{}', status code: {}", request, response.statusCode())
            null
        } else {
            response?.body()
        }
    } ?: throw RequestException("Couldn't fetch '$uri'")

    return body
}
