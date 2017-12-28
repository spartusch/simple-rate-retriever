package com.github.spartusch.rateretriever.rate.v1.service;

import javax.servlet.http.HttpServletRequest;

public interface IqyFileService {

    byte[] generateIqyContentForRequest(final HttpServletRequest request, final String iqyUrlDiscriminator);

    String getIqyFileName(final String provider, final String symbol, final String currencyCode, final String locale);

}
