package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.webquery.WebQueryFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class WebQueryServiceImpl implements WebQueryService {

    private final WebQueryFactory webQueryFactory;

    public WebQueryServiceImpl(final WebQueryFactory webQueryFactory) {
        this.webQueryFactory = webQueryFactory;
    }

    @Override
    public HttpEntity<byte[]> getWebQueryEntity(final String uri, final String symbol, final String currencyCode) {
        final var webQuery = webQueryFactory.create(uri);
        final var fileName = String.format("%s_%s.iqy", symbol, currencyCode.toUpperCase());
        final var headers = new HttpHeaders();

        headers.set(HttpHeaders.CONTENT_TYPE, webQuery.getContentType());
        headers.setContentLength(webQuery.getContentLength());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, webQuery.getContentDisposition(fileName));

        return new HttpEntity<>(webQuery.getContent(), headers);
    }
}
