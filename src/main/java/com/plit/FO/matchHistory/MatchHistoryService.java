package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    public SummonerDTO getAccountBySummonerName(String summonerName) {
        try {
            String encodedName = URLEncoder.encode(summonerName.trim(), StandardCharsets.UTF_8);
            String url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + encodedName + "?api_key=" + riotApiKey;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            SummonerDTO dto = new SummonerDTO();
            dto.setGameName((String) body.get("name")); // 소환사명
            dto.setPuuid((String) body.get("puuid"));
            dto.setProfileIconId((Integer) body.get("profileIconId"));

            return dto;

        } catch (Exception e) {
            System.err.println("소환사 정보 조회 실패:");
            e.printStackTrace();
            return null;
        }
    }


    public SummonerDTO getAccountByRiotId(String gameName, String tagLine) {
        // riotId -> puuid 조회

        String encodedGameName = URLEncoder.encode(gameName.trim(), StandardCharsets.UTF_8);
        String encodedTagLine = URLEncoder.encode(tagLine.trim(), StandardCharsets.UTF_8);

        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                encodedGameName + "/" + encodedTagLine + "?api_key=" + riotApiKey;

        System.out.println("gameName 원본: [" + gameName + "]");
        System.out.println("tagLine 원본: [" + tagLine + "]");

        System.out.println("최종 Riot API 요청 URL: " + url);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, String> body = response.getBody();

            String puuid = body.get("puuid");

            // puuid -> Summoner API
            String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/"
                    + puuid + "?api_key=" + riotApiKey;

            ResponseEntity<Map> summonerResponse = restTemplate.getForEntity(summonerUrl, Map.class);
            Map<String, Object> summonerBody = summonerResponse.getBody();

            // 결과 DTO 구성
            SummonerDTO dto = new SummonerDTO();
            dto.setGameName(body.get("gameName"));
            dto.setTagLine(body.get("tagLine"));
            dto.setPuuid(puuid);
            dto.setProfileIconId((Integer) summonerBody.get("profileIconId"));

            return dto;

        } catch (Exception e) {
            System.err.println("소환사 정보 조회 실패: " + e.getMessage());
            return null;
        }
    }


    // matchid ->
    public List<MatchHistoryDTO> getMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);
        List<MatchHistoryDTO> result = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                String url = "https://asia.api.riotgames.com/lol/match/v5/matches/"
                        + matchId + "?api_key=" + riotApiKey;

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> matchData = response.getBody();
                Map<String, Object> info = (Map<String, Object>) matchData.get("info");
                List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");

                for (Map<String, Object> p : participants) {
                    if (puuid.equals(p.get("puuid"))) {
                        MatchHistoryDTO dto = MatchHistoryDTO.builder()
                                .matchId(matchId)
                                .win((Boolean) p.get("win"))
                                .teamPosition((String) p.get("teamPosition"))
                                .championName((String) p.get("championName"))
                                .kills((int) p.get("kills"))
                                .deaths((int) p.get("deaths"))
                                .assists((int) p.get("assists"))
                                .summonerName((String) p.get("summonerName"))
                                .totalDamageDealtToChampions((int) p.get("totalDamageDealtToChampions"))
                                .totalDamageTaken((int) p.get("totalDamageTaken"))
                                .gameMode((String) info.get("gameMode"))
                                .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                        ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                                .itemIds(List.of(
                                        String.valueOf(p.get("item0")),
                                        String.valueOf(p.get("item1")),
                                        String.valueOf(p.get("item2")),
                                        String.valueOf(p.get("item3")),
                                        String.valueOf(p.get("item4")),
                                        String.valueOf(p.get("item5")),
                                        String.valueOf(p.get("item6"))
                                ))
                                .build();

                        result.add(dto);
                        break;
                    }
                }

            } catch (Exception e) {
                System.err.println("매치 데이터 조회 실패 (" + matchId + "): " + e.getMessage());
            }
        }

        return result;
    }

    // puuid → 최근 match ID 5개 조회
    public List<String> getMatchIdsByPuuid(String puuid) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=5&api_key=" + riotApiKey;

            String[] matchIds = restTemplate.getForObject(url, String[].class);
            return Arrays.asList(matchIds);
        } catch (Exception e) {
            System.err.println("매치 ID 조회 실패: " + e.getMessage());
            return List.of();
        }
    }
}
