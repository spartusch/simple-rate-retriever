package com.github.spartusch.rateretriever.rate.v1.provider

import com.github.spartusch.rateretriever.rate.v1.exception.RequestException
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val USER_AGENT = "User Agent"

private val log = LoggerFactory.getLogger("RateProviderExtensionsKt")

@Suppress("MagicNumber")
private val validStatusCodeRange = 1..399

internal fun HttpClient.getUrl(uri: URI, accept: String, requestTimer: Timer): String {
    val request = HttpRequest.newBuilder(uri)
            .header("User-Agent", USER_AGENT)
            .header("Accept", accept)
            .build()
    log.info("Fetching {} as '{}' ...", uri, accept)
    return requestTimer.recordCallable {
        this.send(request, HttpResponse.BodyHandlers.ofString())
                .also { log.info("Response status code: {}", it.statusCode()) }
                .takeIf { it.statusCode() in validStatusCodeRange }
                ?.body()
    } ?: throw RequestException("Couldn't fetch $uri")
}
