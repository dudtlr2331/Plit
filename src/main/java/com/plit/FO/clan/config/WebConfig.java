package com.plit.FO.clan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 개발 중: 실제 파일 저장 폴더 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/Users/minseok/dev/plit-image/");
    }
}