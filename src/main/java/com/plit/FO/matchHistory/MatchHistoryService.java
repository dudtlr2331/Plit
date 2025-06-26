package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final RestTemplate restTemplate;

    @Value("${external.riot.api-key}")
    private String riotApiKey;

    // 소환사 명 -> puuid ( 소환사 마다 riot api 의 고유 아이디 ) 조회
    public String getPuuidBySummonerName(String summonerName) {
        String url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + summonerName + "?api_key=" + riotApiKey;

        SummonerDTO summoner = restTemplate.getForObject(url, SummonerDTO.class);
        System.out.println("summoner : " + summoner);

        return summoner.getPuuid();
    }

    // puuid -> matchId
    public List<String> getMatchIdsByPuuid(String puuid) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/" + puuid
                + "/ids?start=0&count=5&api_key=" + riotApiKey;

        String[] matchIds = restTemplate.getForObject(url, String[].class);
        System.out.println("matchIds " + Arrays.toString(matchIds));

        return List.of(restTemplate.getForObject(url, String[].class));
    }

}
