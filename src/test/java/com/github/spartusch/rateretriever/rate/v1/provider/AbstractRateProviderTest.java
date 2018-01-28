package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import wiremock.org.apache.http.HttpHeaders;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class AbstractRateProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private AbstractRateProvider provider;

    @Before
    public void setUp() {
        provider = new AbstractRateProvider() {};
    }

    @Test
    public void test_getUrl_happyCase() {
        stubFor(get(urlEqualTo("/some/url"))
                .withHeader(HttpHeaders.ACCEPT, equalTo("text/html"))
                .withHeader(HttpHeaders.USER_AGENT, matching("[a-zA-Z ]+"))
                .willReturn(aResponse().withStatus(200).withBody("response")));

        final Mono<String> response = provider.getUrl(wireMockRule.url("/some/url"), "text/html", String.class);

        StepVerifier.create(response)
                .expectNext("response")
                .verifyComplete();
    }

    @Test
    public void test_getUrl_errorCase() {
        stubFor(get(urlEqualTo("/some/url")).willReturn(aResponse().withStatus(404)));
        final Mono<String> response = provider.getUrl(wireMockRule.url("/some/url"), "text/html", String.class);
        StepVerifier.create(response)
                .verifyErrorMatches(e -> e.getMessage().contains("Not Found"));
    }

    @Test
    public void test_getUrl_redirect() {
        stubFor(get(urlEqualTo("/some/url"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/another/url").withBody("not here")));
        stubFor(get(urlEqualTo("/another/url"))
                .willReturn(aResponse().withStatus(200).withBody("response")));

        final Mono<String> response = provider.getUrl(wireMockRule.url("/some/url"), "text/html", String.class);

        StepVerifier.create(response)
                .expectNext("response")
                .verifyComplete();
    }

    @Test
    public void test_getUrl_redirectOnlyOnce() {
        stubFor(get(urlEqualTo("/url1"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/url2").withBody("content1")));
        stubFor(get(urlEqualTo("/url2"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/url3").withBody("content2")));
        stubFor(get(urlEqualTo("/url3"))
                .willReturn(aResponse().withStatus(200).withBody("content3")));

        final Mono<String> response = provider.getUrl(wireMockRule.url("/url1"), "text/html", String.class);

        StepVerifier.create(response)
                .expectNext("content2")
                .verifyComplete();
    }

    @Test
    public void test_toBigDecimal_happyCase_localeEnglish() {
        StepVerifier.create(provider.toBigDecimal(Locale.ENGLISH, "1,234.567"))
                .expectNextMatches(result -> result.compareTo(new BigDecimal("1234.567")) == 0)
                .verifyComplete();
    }

    @Test
    public void test_toBigDecimal_happyCase_localeGerman() {
        StepVerifier.create(provider.toBigDecimal(Locale.GERMAN, "1.234,567"))
                .expectNextMatches(result -> result.compareTo(new BigDecimal("1234.567")) == 0)
                .verifyComplete();
    }

    @Test
    public void test_toBigDecimal_errorCase() {
        StepVerifier.create(provider.toBigDecimal(Locale.ENGLISH, "noAmount"))
                .verifyError(ParseException.class);
    }
}
