package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

        final Maybe<String> response = provider.getUrl(wireMockRule.url("/some/url"), "text/html");

        response.test().assertResult("response");
    }

    @Test
    public void test_getUrl_errorCase() {
        stubFor(get(urlEqualTo("/some/url")).willReturn(aResponse().withStatus(404)));
        final Maybe<String> response = provider.getUrl(wireMockRule.url("/some/url"), "text/html");
        response.test().assertErrorMessage("Not Found");
    }

    @Test
    public void test_toBigDecimal_happyCase_localeEnglish() {
        provider.toBigDecimal(Locale.ENGLISH, "1,234.567")
                .test()
                .assertValue(result -> result.compareTo(new BigDecimal("1234.567")) == 0)
                .assertComplete();
    }

    @Test
    public void test_toBigDecimal_happyCase_localeGerman() {
        provider.toBigDecimal(Locale.GERMAN, "1.234,567")
                .test()
                .assertValue(result -> result.compareTo(new BigDecimal("1234.567")) == 0)
                .assertComplete();
    }

    @Test
    public void test_toBigDecimal_errorCase() {
        provider.toBigDecimal(Locale.ENGLISH, "noAmount")
                .test()
                .assertError(ParseException.class);
    }
}
