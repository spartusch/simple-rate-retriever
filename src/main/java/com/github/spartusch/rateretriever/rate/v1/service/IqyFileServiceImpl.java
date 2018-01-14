package com.github.spartusch.rateretriever.rate.v1.service;

import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.Charset;

@Service
public class IqyFileServiceImpl implements IqyFileService {

    @Override
    public byte[] generateIqyContentForRequest(final HttpRequest request, final String iqyUrlDiscriminator) {
        assert iqyUrlDiscriminator != null;

        final URI uri = request.getURI();
        final StringBuilder sb = new StringBuilder(uri.toString());

        final int iqyUrlDiscriminatorIndex = sb.lastIndexOf(iqyUrlDiscriminator);
        if (iqyUrlDiscriminatorIndex > -1) {
            sb.delete(iqyUrlDiscriminatorIndex, sb.length());
        }

        if (uri.getQuery() != null) {
            sb.append("?").append(uri.getQuery());
        }

        sb.append("\r\n");

        return sb.toString().getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public String getIqyFileName(final String provider, final String symbol, final String currencyCode, final String locale) {
        return String.format("%s_%s.iqy", symbol, currencyCode.toUpperCase());
    }
}
