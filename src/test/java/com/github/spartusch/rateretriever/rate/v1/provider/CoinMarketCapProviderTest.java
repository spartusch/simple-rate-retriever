package com.github.spartusch.rateretriever.rate.v1.provider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class CoinMarketCapProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private CoinMarketCapRateProvider provider;

    @Before
    public void setUp() {
        provider = new CoinMarketCapRateProvider(wireMockRule.url(""));
    }

    private void stubWithJsonResponse(final String url, final int statusCode, final String response) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response))
        );
    }

    //
    //  getCurrentRate
    //

    @Test
    public void test_getCurrentRate_bitcoin_EUR_success() {
        stubWithJsonResponse("/v1/ticker/bitcoin?convert=EUR",
                200,
                "{\n\"price_usd\": \"14,150.1367\",\n\"price_eur\": \"11,230.7300\"\n}");
        final BigDecimal result = provider.getCurrentRate("bitcoin", "EUR").blockingGet();
        assertThat(result).isEqualByComparingTo("11230.73");
    }

    @Test
    public void test_getCurrentRate_bitcoin_USD_success() {
        stubWithJsonResponse("/v1/ticker/bitcoin?convert=USD",
                200,
                "{\n\"price_usd\": \"14,150.1367\",\n\"price_eur\": \"11,230.7300\"\n}");
        final BigDecimal result = provider.getCurrentRate("bitcoin", "USD").blockingGet();
        assertThat(result).isEqualByComparingTo("14150.1367");
    }

    @Test
    public void test_getCurrentRate_bitcoin_missing_currencyCode() {
        stubWithJsonResponse("/v1/ticker/bitcoin?convert=",
                200,
                "{\n\"price_usd\": \"14,150.1367\"\n}");
        provider.getCurrentRate("bitcoin", "")
                .test()
                .assertError(RuntimeException.class)
                .assertErrorMessage("Amount not found");
    }

    @Test
    public void test_getCurrentRate_bitcoin_unknown_currencyCode() {
        stubWithJsonResponse("/v1/ticker/bitcoin?convert=XXX",
                200,
                "{\n\"price_usd\": \"14,150.1367\",\n}");
        provider.getCurrentRate("bitcoin", "XXX")
                .test()
                .assertError(RuntimeException.class)
                .assertErrorMessage("Amount not found");
    }

    @Test
    public void test_getCurrentRate_unknown_symbol() {
        stubWithJsonResponse("/v1/ticker/foobar?convert=EUR",
                404,
                "{\n \"error\": \"id not found\"\n}");
        provider.getCurrentRate("foobar", "EUR")
                .test()
                .assertError(HttpResponseException.class)
                .assertErrorMessage("Not Found");
    }

    @Test
    public void test_getCurrentRate_serverError() {
        stubWithJsonResponse("/v1/ticker/bitcoin?convert=EUR",
                503,
                "");
        provider.getCurrentRate("bitcoin", "EUR")
                .test()
                .assertError(HttpResponseException.class)
                .assertErrorMessage("Service Unavailable");
    }

    //
    //  isCurrencyCodeSupported
    //

    @Test
    public void test_isCurrencyCodeSupported_returnsTrueForAllSpecifiedByCoinMarketCapApi_upperCase() {
        final List<String> supportedCurrencies = Collections.unmodifiableList(Arrays.asList(
                "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR",
                "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PKR", "PLN", "RUB", "SEK", "SGD", "THB", "TRY", "TWD",
                "ZAR", "USD"
        ));
        for (final String currency : supportedCurrencies) {
            assertThat(provider.isCurrencyCodeSupported(currency)).isTrue();
        }
    }

    @Test
    public void test_isCurrencyCodeSupported_returnsTrueForAllSpecifiedByCoinMarketCapApi_lowerCase() {
        final List<String> supportedCurrencies = Collections.unmodifiableList(Arrays.asList(
                "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS", "INR",
                "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PKR", "PLN", "RUB", "SEK", "SGD", "THB", "TRY", "TWD",
                "ZAR", "USD"
        ));
        for (final String currency : supportedCurrencies) {
            assertThat(provider.isCurrencyCodeSupported(currency.toLowerCase())).isTrue();
        }
    }

    @Test
    public void test_isCurrencyCodeSupported_nullCode() {
        assertThat(provider.isCurrencyCodeSupported(null)).isFalse();
    }

    @Test
    public void test_isCurrencyCodeSupported_emptyCode() {
        assertThat(provider.isCurrencyCodeSupported("")).isFalse();
    }

    @Test
    public void test_isCurrencyCodeSupported_unsupportedCode() {
        assertThat(provider.isCurrencyCodeSupported("XXX")).isFalse();
    }
}
