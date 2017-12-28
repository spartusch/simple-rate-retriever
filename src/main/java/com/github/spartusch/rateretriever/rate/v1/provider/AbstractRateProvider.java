package com.github.spartusch.rateretriever.rate.v1.provider;

import io.reactivex.Maybe;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public abstract class AbstractRateProvider {

    private static final String USER_AGENT = "User Agent";

    private static final Logger log = LoggerFactory.getLogger(AbstractRateProvider.class);

    protected Maybe<String> getUrl(final String url, final String accept) {
        log.info("Fetching {}", url);
        try {
            final String content = Request
                    .Get(url)
                    .addHeader("Accept", accept)
                    .addHeader("User-Agent", USER_AGENT)
                    .execute()
                    .returnContent()
                    .asString();
            return Maybe.just(content);
        } catch (IOException e) {
            log.error("Fetch failed: {}", e.getMessage());
            return Maybe.error(e);
        }
    }

    protected Maybe<BigDecimal> toBigDecimal(final Locale locale, final String amount) {
        final NumberFormat numberFormat = NumberFormat.getInstance(locale);
        try {
            return Maybe.just(new BigDecimal(numberFormat.parse(amount).toString()));
        } catch (ParseException e) {
            return Maybe.error(e);
        }
    }

}
