package com.github.spartusch.rateretriever.rate.v1.controller;

import com.github.spartusch.rateretriever.rate.v1.service.IqyFileService;
import com.github.spartusch.rateretriever.rate.v1.service.RateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateService rateService;

    @MockBean
    private IqyFileService iqyFileService;

    //
    // getStockExchangeRate
    //

    @Test
    public void test_getStockExchangeRate_happyCase() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "de-DE"))
                .willReturn("123,0000");
        mockMvc.perform(get("/rate/v1/stockexchange/ETF110/EUR?locale=de-DE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andExpect(content().string("123,0000"));
    }

    @Test
    public void test_getStockExchangeRate_missingLocaleDefaultsToUs() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "EUR", "en-US")).willReturn("");

        mockMvc.perform(get("/rate/v1/stockexchange/ETF110/EUR")).andExpect(status().isOk());

        verify(rateService, times(1))
                .getStockExchangeRate("ETF110", "EUR", "en-US");
    }

    @Test
    public void test_getStockExchangeRate_IllegalArgumentException() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willThrow(new IllegalArgumentException("Error message"));
        mockMvc.perform(get("/rate/v1/stockexchange/ETF110/XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error message"));
    }

    @Test
    public void test_getStockExchangeRate_RuntimeException() throws Exception {
        given(rateService.getStockExchangeRate("ETF110", "XXX", "en-US"))
                .willThrow(new RuntimeException("Error message"));
        mockMvc.perform(get("/rate/v1/stockexchange/ETF110/XXX"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error message"));
    }

    //
    // getCoinMarketRate
    //

    @Test
    public void test_getCoinMarketRate_happyCase() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "de-DE"))
                .willReturn("10.000,0000");
        mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/EUR?locale=de-DE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
                .andExpect(content().string("10.000,0000"));
    }

    @Test
    public void test_getCoinMarketRate_missingLocaleDefaultsToUs() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "EUR", "en-US")).willReturn("");

        mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/EUR")).andExpect(status().isOk());

        verify(rateService, times(1))
                .getCoinMarketRate("bitcoin", "EUR", "en-US");
    }

    @Test
    public void test_getCoinMarketRate_IllegalArgumentException() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willThrow(new IllegalArgumentException("Error message"));
        mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error message"));
    }

    @Test
    public void test_getCoinMarketRate_RuntimeException() throws Exception {
        given(rateService.getCoinMarketRate("bitcoin", "XXX", "en-US"))
                .willThrow(new RuntimeException("Error message"));
        mockMvc.perform(get("/rate/v1/coinmarket/bitcoin/XXX"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error message"));
    }

    //
    // downloadIqyFileForRequest
    //

    @Test
    public void test_downloadIqyFileForRequest_happyCase() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willReturn("filename.iqy");
        given(iqyFileService.generateIqyContentForRequest(any(HttpServletRequest.class), eq("/iqy")))
                .willReturn("content".getBytes());
        mockMvc.perform(get("/rate/v1/provider/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=filename.iqy"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf("content".length())))
                .andExpect(content().string("content"));
    }

    @Test
    public void test_downloadIqyFileForRequest_missingLocaleDefaultsToUs() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "en-US"))
                .willReturn("");
        given(iqyFileService.generateIqyContentForRequest(any(HttpServletRequest.class), eq("/iqy")))
                .willReturn("".getBytes());

        mockMvc.perform(get("/rate/v1/provider/symbol/currency/iqy")).andExpect(status().isOk());

        verify(iqyFileService, times(1))
                .getIqyFileName("provider", "symbol", "currency", "en-US");
    }

    @Test
    public void test_downloadIqyFileForRequest_IllegalArgumentException() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willThrow(new IllegalArgumentException("Error message"));
        mockMvc.perform(get("/rate/v1/provider/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error message"));
    }

    @Test
    public void test_downloadIqyFileForRequest_RuntimeException() throws Exception {
        given(iqyFileService.getIqyFileName("provider", "symbol", "currency", "loc"))
                .willThrow(new RuntimeException("Error message"));
        mockMvc.perform(get("/rate/v1/provider/symbol/currency/iqy?locale=loc"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error message"));
    }
}
