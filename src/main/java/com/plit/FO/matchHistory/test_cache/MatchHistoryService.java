//package com.plit.FO.matchHistory.test_cache;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class MatchHistoryService {
//
//    private final MatchHistoryRepository matchHistoryRepository;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Value("${riot.api.key}")
//    private String riotApiKey;
//
//    public String getPuuidWithCache(String gameName, String tagLine) {
//        Optional<MatchHistoryEntity> cache = matchHistoryRepository.findByGameNameAndTagLine(gameName, tagLine);
//        if (cache.isPresent()) {
//            return cache.get().getPuuid();
//        }
//
//        try {
//            String encodedGameName = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
//            String encodedTagLine = URLEncoder.encode(tagLine, StandardCharsets.UTF_8);
//
//            // 1차: Riot ID 조회 시도
//            String url = String.format("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
//                    encodedGameName, encodedTagLine);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("X-Riot-Token", riotApiKey);
//            HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
//            Map<String, Object> body = response.getBody();
//
//            if (body != null && body.get("puuid") != null) {
//                String puuid = (String) body.get("puuid");
//
//                MatchHistoryEntity entityToSave = MatchHistoryEntity.builder()
//                        .gameName(gameName)
//                        .tagLine(tagLine)
//                        .puuid(puuid)
//                        .build();
//
//                matchHistoryRepository.save(entityToSave);
//                return puuid;
//            }
//        } catch (Exception e) {
//            System.err.println("[Riot ID 조회 실패] " + e.getMessage());
//        }
//
//        // 2차: 역추적 방식 - match/v5에서 puuid 추출
//        try {
//            String fullRiotId = URLEncoder.encode(gameName + "#" + tagLine, StandardCharsets.UTF_8);
//            String matchUrl = "https://asia.api.riotgames.com/lol/match/v5/matches/by-riot-id/" + fullRiotId + "/ids?start=0&count=1";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("X-Riot-Token", riotApiKey);
//            HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//            ResponseEntity<String[]> matchResponse = restTemplate.exchange(matchUrl, HttpMethod.GET, entity, String[].class);
//            String[] matchIds = matchResponse.getBody();
//
//            if (matchIds == null || matchIds.length == 0) return null;
//
//            String matchId = matchIds[0];
//            String detailUrl = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId;
//            ResponseEntity<Map> detailRes = restTemplate.exchange(detailUrl, HttpMethod.GET, entity, Map.class);
//
//            Map<String, Object> matchData = detailRes.getBody();
//            Map<String, Object> info = (Map<String, Object>) matchData.get("info");
//            List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
//
//            for (Map<String, Object> p : participants) {
//                if (gameName.equalsIgnoreCase((String) p.get("summonerName"))) {
//                    String puuid = (String) p.get("puuid");
//
//                    MatchHistoryEntity entityToSave = MatchHistoryEntity.builder()
//                            .gameName(gameName)
//                            .tagLine(tagLine)
//                            .puuid(puuid)
//                            .build();
//                    matchHistoryRepository.save(entityToSave);
//
//                    return puuid;
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("[역추적 실패] " + e.getMessage());
//        }
//
//        return null;
//    }
//}