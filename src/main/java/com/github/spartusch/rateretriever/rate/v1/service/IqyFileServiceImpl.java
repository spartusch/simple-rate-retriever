package com.github.spartusch.rateretriever.rate.v1.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;

@Service
public class IqyFileServiceImpl implements IqyFileService {

    @Override
    public byte[] generateIqyContentForRequest(final HttpServletRequest request, final String iqyUrlDiscriminator) {
        assert iqyUrlDiscriminator != null;

        final StringBuffer sb = request.getRequestURL();

        final int iqyUrlDiscriminatorIndex = sb.lastIndexOf(iqyUrlDiscriminator);
        if (iqyUrlDiscriminatorIndex > -1) {
            sb.delete(iqyUrlDiscriminatorIndex, sb.length());
        }

        final String queryString = request.getQueryString();
        if (queryString != null) {
            sb.append("?").append(queryString);
        }

        sb.append("\r\n");

        return sb.toString().getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public String getIqyFileName(final String provider, final String symbol, final String currencyCode, final String locale) {
        return String.format("%s_%s.iqy", symbol, currencyCode.toUpperCase());
    }
}
