package com.codewithsubhra.money_tracker_backend.currency;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FxProperties.class)
public class CurrencyConfig {

    /** Dedicated RestClient pointed at the FX provider's base URL. */
    @Bean
    RestClient fxRestClient(FxProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
