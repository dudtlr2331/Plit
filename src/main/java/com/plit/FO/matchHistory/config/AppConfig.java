package com.plit.FO.matchHistory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // HTTP 요청도구( riot api 로 get 조회 요청 / post 생성, 전송 요청 )
        return new RestTemplate();
    }
}
