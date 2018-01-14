package com.github.spartusch.rateretriever.rate.v1.controller;

import com.github.spartusch.rateretriever.rate.v1.service.IqyFileService;
import com.github.spartusch.rateretriever.rate.v1.service.RateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebFluxTest(RateController.class)
public class RateControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RateService rateService;

    @MockBean
    private IqyFileService iqyFileService;

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
    public void test_getStockExchangeRate_missingLocaleDefaultsToUs() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "en-US")).willReturn(Mono.empty());

        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/EUR")
                .exchange()
                .expectStatus().isOk();

        verify(rateService, times(1))
                .getStockExchangeRate("ETF110", "EUR", "en-US");
    }

    @Test
    public void test_getStockExchangeRate_IllegalArgumentException() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));
        webTestClient.get().uri("/rate/v1/stockexchange/ETF110/XXX")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_getStockExchangeRate_RuntimeException() throws Exception {
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
    public void test_getCoinMarketRate_happyCase() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "de-DE"))
                .willReturn(Mono.just("10.000,0000"));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR?locale=de-DE")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/plain;charset=UTF-8")
                .expectBody(String.class).isEqualTo("10.000,0000");
    }

    @Test
    public void test_getCoinMarketRate_missingLocaleDefaultsToUs() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "en-US")).willReturn(Mono.empty());

        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/EUR")
                .exchange()
                .expectStatus().isOk();

        verify(rateService, times(1))
                .getCoinMarketRate("bitcoin", "EUR", "en-US");
    }

    @Test
    public void test_getCoinMarketRate_IllegalArgumentException() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willReturn(Mono.error(new IllegalArgumentException("Error message")));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/XXX")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_getCoinMarketRate_RuntimeException() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willReturn(Mono.error(new RuntimeException("Error message")));
        webTestClient.get().uri("/rate/v1/coinmarket/bitcoin/XXX").exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("Error message");
    }

    //
    // downloadIqyFileForRequest
    //

    @Test
    public void test_downloadIqyFileForRequest_happyCase() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willReturn("filename.iqy");
        given(iqyFileService.generateIqyContentForRequest(any(ServerHttpRequest.class), eq("/iqy")))
                .willReturn("content".getBytes());
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=filename.iqy"))
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .expectHeader().contentLength("content".length())
                .expectBody(String.class).isEqualTo("content");
    }

    @Test
    public void test_downloadIqyFileForRequest_missingLocaleDefaultsToUs() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "en-US"))
                .willReturn("");
        given(iqyFileService.generateIqyContentForRequest(any(ServerHttpRequest.class), eq("/iqy")))
                .willReturn("".getBytes());

        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy")
                .exchange()
                .expectStatus().isOk();

        verify(iqyFileService, times(1))
                .getIqyFileName("provider", "symbol", "currency", "en-US");
    }

    @Test
    public void test_downloadIqyFileForRequest_IllegalArgumentException() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willThrow(new IllegalArgumentException("Error message"));
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Error message");
    }

    @Test
    public void test_downloadIqyFileForRequest_RuntimeException() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willThrow(new RuntimeException("Error message"));
        webTestClient.get().uri("/rate/v1/provider/symbol/currency/iqy?locale=loc")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class).isEqualTo("Error message");
    }
}
