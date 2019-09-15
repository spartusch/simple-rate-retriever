package com.github.spartusch.rateretriever.rate.v1.service;

import com.github.spartusch.webquery.WebQueryDefaultFactory;
import com.github.spartusch.webquery.WebQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebQueryFactoryConfiguration {

    @Bean
    public WebQueryFactory webQueryFactory() {
        return new WebQueryDefaultFactory();
    }

}
