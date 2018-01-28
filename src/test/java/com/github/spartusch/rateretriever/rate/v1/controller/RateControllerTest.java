package com.github.spartusch.rateretriever.rate.v1.controller;

import com.github.spartusch.rateretriever.rate.v1.service.RateService;
import com.github.spartusch.webquery.WebQuery;
import com.github.spartusch.webquery.WebQueryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebFluxTest(RateController.class)
@TestPropertySource(properties = {
        "rate.events.interval=10"
})
public class RateControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RateService rateService;

    @MockBean
    private WebQueryService webQueryService;

    //
    // getStockExchangeRate
    //

    @Test
    public void test_getStockExchangeRate_happyCase() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "de-DE"))
                .willReturn(Mono.just("123,0000"));
        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/EUR?locale=de-DE")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/plain;charset=UTF-8")
                .expectBody(String.class).isEqualTo("123,0000");
    }

    @Test
    public void test_getStockExchangeRate_missingLocaleDefaultsToUs() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "en-US")).willReturn(Mono.empty());

        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/EUR")
                .exchange()
                .expectStatus().isOk();

        verify(rateService, times(1))
                .getStockExchangeRate("ETF110", "EUR", "en-US");
    }

    @Test
    public void test_getStockExchangeRate_IllegalArgumentException() {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));
        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/XXX")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_getStockExchangeRate_RuntimeException() {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willReturn(Mono.error(new RuntimeException("Error message")));
        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/XXX")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("Error message");
    }

    //
    // getCoinMarketRate
    //

    @Test
    public void test_getCoinMarketRate_happyCase() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "de-DE"))
                .willReturn(Mono.just("10.000,0000"));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR?locale=de-DE")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/plain;charset=UTF-8")
                .expectBody(String.class).isEqualTo("10.000,0000");
    }

    @Test
    public void test_getCoinMarketRate_missingLocaleDefaultsToUs() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "en-US")).willReturn(Mono.empty());

        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR")
                .exchange()
                .expectStatus().isOk();

        verify(rateService, times(1))
                .getCoinMarketRate("bitcoin", "EUR", "en-US");
    }

    @Test
    public void test_getCoinMarketRate_IllegalArgumentException() {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/XXX")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_getCoinMarketRate_RuntimeException() {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willReturn(Mono.error(new RuntimeException("Error message")));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/XXX").exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("Error message");
    }

    //
    // getCoinMarketRatesStream
    //

    @Test
    public void test_getCoinMarketRatesStream_happyCase() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "de-DE"))
                .willReturn(Mono.just("10.000,0000"))
                .willReturn(Mono.just("10.000,0000"))
                .willReturn(Mono.just("10.000,0000"))
                .willReturn(Mono.just("11.000,0000"))
                .willReturn(Mono.just("12.000,0000"));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR?locale=de-DE")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getResponseHeaders().get("Content-Type").get(0)).isEqualTo("text/event-stream");
        StepVerifier.create(result.getResponseBody())
                .expectNext("10.000,0000", "11.000,0000", "12.000,0000")
                .thenCancel()
                .verify();
    }

    @Test
    public void test_getCoinMarketRatesStream_missingLocaleDefaultsToUs() {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "en-US")).willReturn(Mono.just("123"));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        StepVerifier.create(result.getResponseBody()).thenCancel();
        verify(rateService, atLeastOnce()).getCoinMarketRate("bitcoin", "EUR", "en-US");
    }

    @Test
    public void test_getCoinMarketRatesStream_IllegalArgumentException() {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/XXX")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseHeaders().get("Content-Type").get(0)).isEqualTo("text/event-stream");
        StepVerifier.create(result.getResponseBody())
                .expectNext("Error message")
                .verifyComplete();
    }

    //
    // getStockExchangeRatesStream
    //

    @Test
    public void test_getStockExchangeRatesStream_happyCase() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "de-DE"))
                .willReturn(Mono.just("46,0000"))
                .willReturn(Mono.just("46,0000"))
                .willReturn(Mono.just("47,0000"))
                .willReturn(Mono.just("48,0000"));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/stockexchange/ETF110/EUR?locale=de-DE")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getResponseHeaders().get("Content-Type").get(0)).isEqualTo("text/event-stream");
        StepVerifier.create(result.getResponseBody())
                .expectNext("46,0000", "47,0000", "48,0000")
                .thenCancel()
                .verify();
    }

    @Test
    public void test_getStockExchangeRatesStream_missingLocaleDefaultsToUs() {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "en-US")).willReturn(Mono.just("123"));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/stockexchange/ETF110/EUR")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        StepVerifier.create(result.getResponseBody()).thenCancel();
        verify(rateService, atLeastOnce()).getStockExchangeRate("ETF110", "EUR", "en-US");
    }

    @Test
    public void test_getStockExchangeRatesStream_IllegalArgumentException() {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));

        final FluxExchangeResult<String> result = webTestClient.get().uri("/rate/v1/stockexchange/ETF110/XXX")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseHeaders().get("Content-Type").get(0)).isEqualTo("text/event-stream");
        StepVerifier.create(result.getResponseBody())
                .expectNext("Error message")
                .verifyComplete();
    }

    //
    // downloadIqyFileForRequest
    //

    @Test
    public void test_downloadIqyFileForRequest_happyCase() {
        given(webQueryService.createWebQuery("/rate/v1/provider/symbol/currency/iqy?locale=loc", "/iqy"))
                .willReturn(new WebQuery("content".getBytes(), Charset.forName("UTF-8"), "filename.iqy"));
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=symbol_CURRENCY.iqy"))
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .expectHeader().contentLength("content".length())
                .expectBody(String.class).isEqualTo("content");
    }

    @Test
    public void test_downloadIqyFileForRequest_IllegalArgumentException() {
        given(webQueryService.createWebQuery("/rate/v1/provider/symbol/currency/iqy?locale=loc", "/iqy"))
                .willThrow(new IllegalArgumentException("Error message"));
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_downloadIqyFileForRequest_RuntimeException() {
        given(webQueryService.createWebQuery("/rate/v1/provider/symbol/currency/iqy?locale=loc", "/iqy"))
                .willThrow(new RuntimeException("Error message"));
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("Error message");
    }
}
