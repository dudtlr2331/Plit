package com.plit.FO.matchHistory.test;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiotApiService {

    @Value("${riot.api.key}")
    private String riotApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1단계: Riot ID로 PUUID 조회
    public String getPuuid(String gameName, String tagLine) {
        String url = String.format("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s", gameName, tagLine);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return (String) response.getBody().get("puuid");
    }

    // 2단계: PUUID로 티어 조회
    public List<Map<String, Object>> getRankInfoByPuuid(String puuid) {
        String url = String.format("https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/%s", puuid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
        return response.getBody();
    }

    // 한 번에 처리하는 메서드
    public List<Map<String, Object>> fetchTierByRiotId(String gameName, String tagLine) {
        String puuid = getPuuid(gameName, tagLine);
        return getRankInfoByPuuid(puuid);
    }
}


