package com.plit.FO.matchHistory.test;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/riot")
@RequiredArgsConstructor
public class RiotTestController {

    private final RiotApiService riotApiService;

    @GetMapping("/tier")
    public ResponseEntity<?> getTier(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        try {
            List<Map<String, Object>> result = riotApiService.fetchTierByRiotId(gameName, tagLine);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("에러 발생: " + e.getMessage());
        }
    }
}

