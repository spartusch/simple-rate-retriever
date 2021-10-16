package com.github.spartusch.rateretriever.application.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

@Configuration
class BeanConfiguration {
    @Bean
    fun httpClient(): HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
}
