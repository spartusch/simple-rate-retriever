package com.github.spartusch.rateretriever.rate.v1.controller;

import com.github.spartusch.rateretriever.rate.v1.provider.RateProviderType;
import com.github.spartusch.rateretriever.rate.v1.service.IqyFileService;
import com.github.spartusch.rateretriever.rate.v1.service.RateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

@RestController
@RequestMapping("/rate/v1")
public class RateController {

    public static final String DEFAULT_LOCALE = "en-US";

    private static final Logger log = LoggerFactory.getLogger(RateController.class);

    private RateService rateService;
    private IqyFileService iqyFileService;

    @Autowired
    public RateController(final RateService rateService, final IqyFileService iqyFileService) {
        this.rateService = rateService;
        this.iqyFileService = iqyFileService;
    }

    @GetMapping(value = "/coinmarket/{symbol}/{currency}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getCoinMarketRate(@PathVariable("symbol") final String symbol,
                                    @PathVariable("currency") final String currencyCode,
                                    @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return handleRateRequest(RateProviderType.COIN_MARKET, symbol, currencyCode, locale,
                () -> rateService.getCoinMarketRate(symbol, currencyCode, locale)
        );
    }

    @GetMapping(value = "/stockexchange/{symbol}/{currency}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getStockExchangeRate(@PathVariable("symbol") final String symbol,
                                       @PathVariable("currency") final String currencyCode,
                                       @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale) {
        return handleRateRequest(RateProviderType.STOCK_EXCHANGE, symbol, currencyCode, locale,
                () -> rateService.getStockExchangeRate(symbol, currencyCode, locale)
        );
    }

    @GetMapping(value = "/{provider}/{symbol}/{currency}/iqy")
    public HttpEntity<byte[]> downloadIqyFileForRequest(@PathVariable("provider") final String provider,
                                                        @PathVariable("symbol") final String symbol,
                                                        @PathVariable("currency") final String currencyCode,
                                                        @RequestParam(value = "locale", defaultValue = DEFAULT_LOCALE) final String locale,
                                                        final HttpServletRequest request) {
        log.info("IQY request: {}", request.getRequestURI());

        final byte[] fileContent = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        final String fileName = iqyFileService.getIqyFileName(provider, symbol, currencyCode, locale);

        log.info("Sending IQY file '{}'", fileName);
        log.debug("Content of '{}':\n{}", fileName, fileContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.setContentLength(fileContent.length);

        return new HttpEntity<>(fileContent, headers);
    }

    private String handleRateRequest(final String type,
                                     final String symbol,
                                     final String currencyCode,
                                     final String locale,
                                     final Supplier<String> rateSuppler) {
        log.info("Request: '{}', '{}', '{}', '{}'", type, symbol, currencyCode, locale);
        final String response = rateSuppler.get();
        log.info("Response: '{}'", response);
        return response;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleException(final IllegalArgumentException ex) {
        log.error(ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(final RuntimeException ex) {
        log.error(ex.getMessage());
        return ex.getMessage();
    }
}
