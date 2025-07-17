package com.plit.FO.clan.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class ClanWebConfig implements WebMvcConfigurer {

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    @PostConstruct
    public void validateUploadDir() {
        if (uploadDir == null || uploadDir.isBlank()) {
            System.err.println("Clan 이미지 업로드 경로가 비어있습니다.");
            return;
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            System.err.println("Clan 이미지 폴더가 존재하지 않습니다: " + uploadDir);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/upload/clan/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}