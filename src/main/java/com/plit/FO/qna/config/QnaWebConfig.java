package com.plit.FO.qna.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class QnaWebConfig implements WebMvcConfigurer {

    @Value("${custom.upload-path.qna}")
    private String qnaUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/qna/**")
                .addResourceLocations("file:" + qnaUploadPath + "/");
    }
}