
package com.example.bankingbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FraudClientConfig {

    @Bean
    public RestTemplate fraudRestTemplate() {
        return new RestTemplate();
    }
}

