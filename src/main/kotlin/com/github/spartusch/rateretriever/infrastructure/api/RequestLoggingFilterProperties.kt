package com.github.spartusch.rateretriever.infrastructure.api

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.request-logging-filter")
data class RequestLoggingFilterProperties(
    val enabled: Boolean = true,
    val exclude: List<String> = emptyList()
)
