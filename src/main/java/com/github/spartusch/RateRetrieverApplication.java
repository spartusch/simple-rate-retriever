package com.github.spartusch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableAutoConfiguration(exclude = { ErrorWebFluxAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@EnableCaching
public class RateRetrieverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateRetrieverApplication.class, args);
    }

}
