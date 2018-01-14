package com.github.spartusch.rateretriever.rate.v1.service;

import org.springframework.http.HttpRequest;

public interface IqyFileService {

    byte[] generateIqyContentForRequest(final HttpRequest request, final String iqyUrlDiscriminator);

    String getIqyFileName(final String provider, final String symbol, final String currencyCode, final String locale);

}
