package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String apiKey;

    public SummonerDTO getAccountByRiotId(String gameName, String tagLine) {
        // riotId -> puuid 조회
        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                UriUtils.encode(gameName, StandardCharsets.UTF_8) + "/" +
                UriUtils.encode(tagLine, StandardCharsets.UTF_8) +
                "?api_key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, String> body = response.getBody();

            String puuid = body.get("puuid");

            // puuid -> Summoner API
            String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/"
                    + puuid + "?api_key=" + apiKey;

            ResponseEntity<Map> summonerResponse = restTemplate.getForEntity(summonerUrl, Map.class);
            Map<String, Object> summonerBody = summonerResponse.getBody();

            // 🔁 결과 DTO 구성
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

    // puuid → 최근 match ID 5개 조회
    public List<String> getMatchIdsByPuuid(String puuid) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=5&api_key=" + apiKey;

            String[] matchIds = restTemplate.getForObject(url, String[].class);
            return Arrays.asList(matchIds);
        } catch (Exception e) {
            System.err.println("매치 ID 조회 실패: " + e.getMessage());
            return List.of();
        }
    }
}
