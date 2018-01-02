package com.github.spartusch.rateretriever.rate.v1.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
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

    private HttpServletRequest mockRequestFor(final String url, final String queryString) {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        when(request.getQueryString()).thenReturn(queryString);
        return request;
    }

    @Test
    public void test_generateIqyContentForRequest_happyCase() {
        final HttpServletRequest request = mockRequestFor("http://foo/bar/iqy", "a=b");
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar?a=b\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_happyCase_noQueryString() {
        final HttpServletRequest request = mockRequestFor("http://foo/bar/iqy", null);
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_noDiscriminatorInUrl() {
        final HttpServletRequest request = mockRequestFor("http://foo/bar", null);
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "/iqy");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test
    public void test_generateIqyContentForRequest_noDiscriminator() {
        final HttpServletRequest request = mockRequestFor("http://foo/bar", null);
        final byte[] result = iqyFileService.generateIqyContentForRequest(request, "");
        assertThat(result).isEqualTo("http://foo/bar\r\n".getBytes(Charset.forName("UTF-8")));
    }

    @Test(expected = AssertionError.class)
    public void test_generateIqyContentForRequest_nullDiscriminator() {
        final HttpServletRequest request = mockRequestFor("http://foo/bar", null);
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
