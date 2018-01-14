package com.github.spartusch.rateretriever.rate.v1.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class IqyFileServiceImplTest {

    private IqyFileServiceImpl iqyFileService;

    @Before
    public void setUp() {
        iqyFileService = new IqyFileServiceImpl();
    }

    //
    // generateIqyContentForRequest
    //

    private HttpRequest mockRequestFor(final String url) {
        try {
            final HttpRequest request = Mockito.mock(HttpRequest.class);
            when(request.getURI()).thenReturn(new URI(url));
            return request;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_generateIqyContentForRequest_happyCase() {
        final HttpRequest request = mockRequestFor("http://foo/bar/iqy?a=b");
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar?a=b\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_happyCase_noQueryString() {
        final HttpRequest request = mockRequestFor("http://foo/bar/iqy");
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_noDiscriminatorInUrl() {
        final HttpRequest request = mockRequestFor("http://foo/bar");
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_noDiscriminator() {
        final HttpRequest request = mockRequestFor("http://foo/bar");
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test(expected = AssertionError.class)
    public void test_generateIqyContentForRequest_nullDiscriminator() {
        final HttpRequest request = mockRequestFor("http://foo/bar");
        iqyFileService.generateIqyContentForRequest(request, null);
    }

    //
    // getIqyFileName
    //

    @Test
    public void test_getIqyFileName_happyCase() {
        final String result = iqyFileService.getIqyFileName("provider",
                "symbol", "currency", "locale");
        assertThat(result).isEqualTo("symbol_CURRENCY.iqy");
    }
}
