package com.github.spartusch.rateretriever.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication(scanBasePackages = [
    "com.github.spartusch.rateretriever.application",
    "com.github.spartusch.rateretriever.infrastructure"
])
@ConfigurationPropertiesScan(basePackages = [
    "com.github.spartusch.rateretriever.application.configuration",
    "com.github.spartusch.rateretriever.infrastructure"
])
class RateRetrieverApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    SpringApplication.run(RateRetrieverApplication::class.java, *args)
}
