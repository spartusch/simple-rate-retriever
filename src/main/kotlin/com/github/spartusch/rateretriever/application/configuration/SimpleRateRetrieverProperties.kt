package com.github.spartusch.rateretriever.application.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever")
data class SimpleRateRetrieverProperties(
    val fractionDigits: Int
)
