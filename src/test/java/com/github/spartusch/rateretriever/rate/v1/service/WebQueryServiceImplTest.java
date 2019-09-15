package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.webquery.WebQuery;
import com.github.spartusch.webquery.WebQueryFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

public class WebQueryServiceImplTest {

    private WebQueryFactory factory;
    private WebQuery webQuery;

    private WebQueryServiceImpl service;

    @Before
    public void setUp() {
        webQuery = new WebQuery("content", StandardCharsets.UTF_8);
        factory = Mockito.mock(WebQueryFactory.class);
        service = new WebQueryServiceImpl(factory);
        when(factory.create("uri")).thenReturn(webQuery);
    }

    @Test
    public void test_getWebQueryEntity_content() {
        final var entity = service.getWebQueryEntity("uri", "symbol", "currencyCode");
        assertThat(entity.getBody()).isEqualTo(webQuery.getContent());
    }

    @Test
    public void test_getWebQueryEntity_contentType() {
        final var entity = service.getWebQueryEntity("uri", "symbol", "currencyCode");
        assertThat(entity.getHeaders()).contains(entry("Content-Type", singletonList(webQuery.getContentType())));
    }

    @Test
    public void test_getWebQueryEntity_contentLength() {
        final var entity = service.getWebQueryEntity("uri", "symbol", "currencyCode");
        assertThat(entity.getHeaders()).contains(entry("Content-Length", singletonList(String.valueOf(webQuery.getContentLength()))));
    }

    @Test
    public void test_getWebQueryEntity_contentDisposition() {
        final var entity = service.getWebQueryEntity("uri", "symbol", "currencyCode");
        final var expectedContentDisposition = webQuery.getContentDisposition("symbol_CURRENCYCODE.iqy");
        assertThat(entity.getHeaders()).contains(entry("Content-Disposition", singletonList(expectedContentDisposition)));
    }
}
