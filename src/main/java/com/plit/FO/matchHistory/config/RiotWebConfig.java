package com.plit.FO.matchHistory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RiotWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**") // URL 경로와 실제 파일 경로 연결
                .addResourceLocations("classpath:/static/images/"); // /images/로 시작하는 요청은 static/images/ 폴더에서 찾아 응답
    }
}

