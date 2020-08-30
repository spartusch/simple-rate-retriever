package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.exception.RequestException
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

internal fun HttpClient.getUrl(
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

    log.debug("Fetching {} as '{}' ...", uri, accept)

    val body = requestTimer.recordCallable {
        this.send(request, HttpResponse.BodyHandlers.ofString())
            .also { log.debug(" -> Status code: {}", it.statusCode()) }
            .takeIf { it.statusCode() in validStatusCodeRange }
            ?.body()
    } ?: throw RequestException("Couldn't fetch $uri")

    log.trace(" -> Body: {}", body)

    return body
}
