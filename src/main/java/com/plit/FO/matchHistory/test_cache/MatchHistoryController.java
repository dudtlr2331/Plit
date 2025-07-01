//package com.plit.FO.matchHistory.test_cache;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/test")
//@RequiredArgsConstructor
//public class MatchHistoryController {
//
//    private final MatchHistoryService matchHistoryService;
//
//    @GetMapping("/puuid")
//    public ResponseEntity<?> getPuuidByGameNameAndTagLine(
//            @RequestParam String gameName,
//            @RequestParam String tagLine
//    ) {
//        String puuid = matchHistoryService.getPuuidWithCache(gameName, tagLine);
//        if (puuid != null) {
//            return ResponseEntity.ok(puuid);
//        } else {
//            return ResponseEntity.badRequest().body("puuid 조회 실패: 존재하지 않거나 Riot API 오류");
//        }
//    }
//}
