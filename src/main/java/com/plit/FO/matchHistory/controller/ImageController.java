package com.plit.FO.matchHistory.controller;

import com.plit.FO.matchHistory.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/image")
public class ImageController {

    private final ImageService imageService;

    // 전체 동기화 - 수동 트리거 [ localhost:8080/admin/image/sync ]
    @PostMapping("/sync")
    public ResponseEntity<String> syncAllImages() {
        imageService.updateAllImages(); // 매주 수요일 자동 실행하는 그거
        return ResponseEntity.ok("모든 이미지 동기화 완료");
    }

    // 특정 타입만 동기화 [ localhost:8080/admin/image/sync/champion ]
    @PostMapping("/sync/{type}")
    public ResponseEntity<String> syncByType(@PathVariable String type) {
        if (!isValidType(type)) {
            return ResponseEntity.badRequest().body("유효하지 않은 타입: " + type);
        }

        String version = imageService.fetchLatestVersion();
        imageService.updateImagesByType(type, version);
        return ResponseEntity.ok("✔ " + type + " 이미지 동기화 완료");
    }

    private boolean isValidType(String type) {
        return type.equals("champion") || type.equals("item") || type.equals("rune") || type.equals("profile-icon");
    }
}