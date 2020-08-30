package com.github.spartusch.rateretriever.rate.v1.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

@Configuration
class BeanConfiguration {
    @Bean
    fun httpClient() = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
}
