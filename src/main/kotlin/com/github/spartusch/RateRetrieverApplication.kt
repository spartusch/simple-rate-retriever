package com.github.spartusch

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication
@ConfigurationPropertiesScan
class RateRetrieverApplication

fun main(
    args: Array<String>
) {
    @Suppress("SpreadOperator")
    SpringApplication.run(RateRetrieverApplication::class.java, *args)
}
