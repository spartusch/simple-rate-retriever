package com.github.spartusch.rateretriever.rate.v1.controller

import com.github.spartusch.rateretriever.rate.v1.configuration.RequestLoggingFilterProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.Mockito
import org.mockito.Mockito.times
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RequestLoggingFilterTest {

    private lateinit var properties: RequestLoggingFilterProperties

    private lateinit var chain: FilterChain
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse

    private lateinit var cut: RequestLoggingFilter

    @BeforeEach
    fun setUp() {
        properties = Mockito.mock(RequestLoggingFilterProperties::class.java)
        cut = RequestLoggingFilter(properties)

        request = Mockito.mock(HttpServletRequest::class.java)
        response = Mockito.mock(HttpServletResponse::class.java)
        chain = Mockito.mock(FilterChain::class.java)
    }

    @Test
    fun doFilter_callsFilterChainWhenEnabled() {
        given(properties.enabled).willReturn(true)
        cut.doFilter(request, response, chain)
        verify(chain, times(1)).doFilter(request, response)
    }

    @Test
    fun doFilter_callsFilterChainWhenDisabled() {
        given(properties.enabled).willReturn(false)
        cut.doFilter(request, response, chain)
        verify(chain, times(1)).doFilter(request, response)
    }

    @Test
    fun doFilter_callsFilterChainWhenExcluded() {
        given(properties.enabled).willReturn(true)
        given(properties.exclude).willReturn(listOf("some/path"))
        given(request.requestURI).willReturn("some/path/which/must/be/excluded")

        cut.doFilter(request, response, chain)

        verify(chain, times(1)).doFilter(request, response)
    }

    @Test
    fun doFilter_callsFilterChainWhenNotExcluded() {
        given(properties.enabled).willReturn(true)
        given(properties.exclude).willReturn(listOf("some/path"))
        given(request.requestURI).willReturn("but/not/this/path")

        cut.doFilter(request, response, chain)

        verify(chain, times(1)).doFilter(request, response)
    }

}
