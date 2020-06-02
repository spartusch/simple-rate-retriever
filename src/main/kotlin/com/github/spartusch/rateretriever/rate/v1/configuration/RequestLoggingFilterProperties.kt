package com.github.spartusch.rateretriever.rate.v1.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

interface RequestLoggingFilterProperties {
    val enabled: Boolean
    val exclude: List<String>
}

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.request-logging-filter")
data class RequestLoggingFilterPropertiesImpl(
    override val enabled: Boolean = true,
    override val exclude: List<String> = emptyList()
) : RequestLoggingFilterProperties
