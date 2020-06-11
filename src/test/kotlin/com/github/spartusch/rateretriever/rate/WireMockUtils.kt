package com.github.spartusch.rateretriever.rate

import com.github.tomakehurst.wiremock.client.WireMock

object WireMockUtils {
    fun stubResponse(
        url: String,
        response: String,
        statusCode: Int
    ): String {
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo(url)).willReturn(
                WireMock.aResponse().withStatus(statusCode).withBody(response)))
        return url
    }
}
