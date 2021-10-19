package com.github.spartusch.rateretriever.infrastructure.provider

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

interface RateProviderProperties {
    val id: String
    val uri: String
}

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.providers.on-vista")
data class OnVistaProperties(
    override val id: String,
    override val uri: String,
    val maxAttempts: Int = 2
) : RateProviderProperties

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.providers.coin-market-cap")
data class CoinMarketCapProperties(
    override val id: String,
    override val uri: String,
    val apiKey: String
) : RateProviderProperties
