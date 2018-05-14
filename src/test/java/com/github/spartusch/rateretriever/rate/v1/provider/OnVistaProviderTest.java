package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class OnVistaProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private OnVistaRateProvider provider;

    @Before
    public void setUp() {
        provider = new OnVistaRateProvider(wireMockRule.url("/api/header/search?q="));
    }

    private void stubWithHtmlResponse(final String url, final int statusCode, final String response) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "text/html")
                        .withBody(response))
        );
    }

    //
    //  getCurrentRate
    //

    @Test
    public void test_getCurrentRate_searchAndRetrieve() {
        final String assetUrl = wireMockRule.url("some/url/for/foo");
        stubWithHtmlResponse("/api/header/search?q=foo",
                200,
                "... \"snapshotlink\":\"" + assetUrl + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo",
                200,
                "... <span class=\"price\">1.230,45 EUR</span> ...");

        provider.getCurrentRate("foo", "EUR").block();

        verify(1, getRequestedFor(urlEqualTo("/api/header/search?q=foo")));
        verify(1, getRequestedFor(urlEqualTo("/some/url/for/foo")));
    }

    @Test
    public void test_getCurrentRate_searchOnlyOncePerSymbol_success() {
        final String assetUrl = wireMockRule.url("some/url/for/foo");
        stubWithHtmlResponse("/api/header/search?q=foo",
                200,
                "... \"snapshotlink\":\"" + assetUrl + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo",
                200,
                "... <span class=\"price\">1.230,45 EUR</span> ...");

        provider.getCurrentRate("foo", "EUR").block();
        provider.getCurrentRate("foo", "EUR").block();
        provider.getCurrentRate("foo", "EUR").block();

        verify(1, getRequestedFor(urlEqualTo("/api/header/search?q=foo")));
        verify(3, getRequestedFor(urlEqualTo("/some/url/for/foo")));
    }

    @Test
    public void test_getCurrentRate_searchSymbolAgainOnFailure() {
        // 1) Search and successful retrieval (= 1 x search, 1 x first asset url)
        stubWithHtmlResponse("/api/header/search?q=foo", 200,
                "... \"snapshotlink\":\"" + wireMockRule.url("some/url/for/foo") + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo", 200, "... <span class=\"price\">1.230,45 EUR</span> ...");
        provider.getCurrentRate("foo", "EUR").block();

        // 2) Second retrieval fails and search is called again, getting a new asset URL
        // (= 1 x first asset url, 1 x search, 1 x second asset url)
        stubWithHtmlResponse("/some/url/for/foo", 404, "");
        stubWithHtmlResponse("/api/header/search?q=foo", 200,
                "... \"snapshotlink\":\"" + wireMockRule.url("some/url/for/foo/NEW") + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo/NEW", 200, "... <span class=\"price\">1.230,45 EUR</span> ...");
        provider.getCurrentRate("foo", "EUR").block();

        // 3) The new URL is cached (= 1 x second asset url)
        provider.getCurrentRate("foo", "EUR").block();

        verify(2, getRequestedFor(urlEqualTo("/api/header/search?q=foo"))); // search
        verify(2, getRequestedFor(urlEqualTo("/some/url/for/foo"))); // first asset url
        verify(2, getRequestedFor(urlEqualTo("/some/url/for/foo/NEW"))); // second asset url
    }

    @Test
    public void test_getCurrentRate_onlyOneRetryIfSearchFailsAndErrorPropagation() {
        stubWithHtmlResponse("/api/header/search?q=foo", 404, "");

        StepVerifier.create(provider.getCurrentRate("foo", "EUR"))
                .verifyErrorMatches(e -> e.getMessage().contains("Not Found"));

        verify(2, getRequestedFor(urlEqualTo("/api/header/search?q=foo")));
    }

    @Test
    public void test_getCurrentRate_onlyOneRetryIfRetrievalFailsAndErrorPropagation() {
        stubWithHtmlResponse("/api/header/search?q=foo", 200,
                "... \"snapshotlink\":\"" + wireMockRule.url("some/url/for/foo") + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo", 404, "");

        StepVerifier.create(provider.getCurrentRate("foo", "EUR"))
                .verifyErrorMatches(e -> e.getMessage().contains("Not Found"));

        verify(2, getRequestedFor(urlEqualTo("/api/header/search?q=foo")));
        verify(2, getRequestedFor(urlEqualTo("/some/url/for/foo")));
    }

    @Test
    public void test_getCurrentRate_search_cannotExtractAssetUrl() {
        stubWithHtmlResponse("/api/header/search?q=foo", 200, "... Nothing interesting here :-( ...");

        StepVerifier.create(provider.getCurrentRate("foo", "EUR"))
                .verifyErrorMessage("Asset not found");
    }

    @Test
    public void test_getCurrentRate_cannotExtractAmount() {
        stubWithHtmlResponse("/api/header/search?q=foo", 200,
                "... \"snapshotlink\":\"" + wireMockRule.url("some/url/for/foo") + "\" ...");
        stubWithHtmlResponse("/some/url/for/foo", 200, "... No amount here! ...");

        StepVerifier.create(provider.getCurrentRate("foo", "EUR"))
                .verifyErrorMessage("Amount not found");
    }

    @Test
    public void test_getCurrentRate_extractFirstEurRate() {
        stubWithHtmlResponse("/api/header/search?q=asset", 200, "\"snapshotlink\":\"" + wireMockRule.url("asset") + "\"");
        stubWithHtmlResponse("/asset", 200,
                "\n\n<span class=\"price\">100,1234 USD</span>" +
                         "\n<span class=\"price\">111,2200 EUR</span>" +
                         "\n<a>Umrechnung:</a> 333,4400 EUR");

        final BigDecimal result = provider.getCurrentRate("asset", "EUR").block();

        assertThat(result).isEqualByComparingTo("111.22");
    }

    @Test
    public void test_getCurrentRate_extractConvertedRateIfRateIsNonEur() {
        stubWithHtmlResponse("/api/header/search?q=asset", 200, "\"snapshotlink\":\"" + wireMockRule.url("asset") + "\"");
        stubWithHtmlResponse("/asset", 200,
                "\n\n<span class=\"price\">123,00 USD</span>" +
                         "\n<a>Umrechnung:</a> 150,00 EUR");

        final BigDecimal result = provider.getCurrentRate("asset", "EUR").block();

        assertThat(result).isEqualByComparingTo("150");
    }

    @Test
    public void test_getCurrentRate_extractDataPushRateInEur() {
        stubWithHtmlResponse("/api/header/search?q=asset", 200, "\"snapshotlink\":\"" + wireMockRule.url("asset") + "\"");
        stubWithHtmlResponse("/asset", 200,
                "\n<span data-push att='a' att=\"b\">999,99</span>\n  \n<span att='c'>USD</span>" +
                         "\n<span data-push att='a' att=\"b\">12.345,12</span>\n  \n<span att='c'>EUR</span>");

        final BigDecimal result = provider.getCurrentRate("asset", "EUR").block();

        assertThat(result).isEqualByComparingTo("12345.12");
    }

    //
    //  isCurrencyCodeSupported
    //

    @Test
    public void test_isCurrencyCodeSupported_success_upperCase() {
        assertThat(provider.isCurrencyCodeSupported("EUR").block()).isTrue();
    }

    @Test
    public void test_isCurrencyCodeSupported_success_lowerCase() {
        assertThat(provider.isCurrencyCodeSupported("eur").block()).isTrue();
    }

    @Test
    public void test_isCurrencyCodeSupported_nullCode() {
        assertThat(provider.isCurrencyCodeSupported(null).block()).isFalse();
    }

    @Test
    public void test_isCurrencyCodeSupported_emptyCode() {
        assertThat(provider.isCurrencyCodeSupported("").block()).isFalse();
    }

    @Test
    public void test_isCurrencyCodeSupported_unsupportedCode1() {
        assertThat(provider.isCurrencyCodeSupported("USD").block()).isFalse();
    }

    @Test
    public void test_isCurrencyCodeSupported_unsupportedCode2() {
        assertThat(provider.isCurrencyCodeSupported("XXX").block()).isFalse();
    }
}
