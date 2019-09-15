package com.github.spartusch.rateretriever.rate.v1.service;

import org.springframework.http.HttpEntity;

public interface WebQueryService {

    HttpEntity<byte[]> getWebQueryEntity(final String uri,
                                         final String symbol,
                                         final String currencyCode);

}
