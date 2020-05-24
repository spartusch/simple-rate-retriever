package com.github.spartusch.rateretriever.rate.v1.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

interface RateProviderProperties {
    val uri: String
}

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.provider.on-vista")
data class OnVistaProperties(override val uri: String, val maxRetries: Int = 1) : RateProviderProperties

@ConstructorBinding
@ConfigurationProperties(prefix = "simple-rate-retriever.provider.coin-market-cap")
data class CoinMarketCapProperties(override val uri: String) : RateProviderProperties
