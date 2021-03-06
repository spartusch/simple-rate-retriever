package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

@Component
class RequestLoggingFilter(private val properties: RequestLoggingFilterProperties) : Filter {

    init {
        log.info("Request logging is {}", if (properties.enabled) "enabled" else "disabled")
        if (properties.enabled) {
            log.info("Paths excluded from request logging: {}", properties.exclude)
        }
    }

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain) {
        val request: HttpServletRequest = servletRequest as HttpServletRequest
        if (properties.enabled && properties.exclude.none { request.requestURI.startsWith(it) }) {
            log.info("Request: {}?{}", request.requestURI, request.queryString)
        }
        chain.doFilter(servletRequest, servletResponse)
    }
}
