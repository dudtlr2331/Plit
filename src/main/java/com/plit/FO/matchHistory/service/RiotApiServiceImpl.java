package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.RankDTO;
import com.plit.FO.matchHistory.dto.SummonerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.plit.FO.matchHistory.service.MatchHelper.toInt;

@Service
@RequiredArgsConstructor
public class RiotApiServiceImpl implements RiotApiService{

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    // riot api 기본정보 가져오기

    // (*) riotId [ 게임이름( 소환사명 ), 태그( # ) ] -> puuid -> 계정정보 [ account/v1 ]
    public SummonerDTO getAccountByRiotId(String gameName, String tagLine) {
        try {
//            // Riot ID -> puuid, gameName, tagLine // uri( 자원 식별 방식 ) 만들기
//            URI riotIdUri = UriComponentsBuilder // URI 생성도구
//                    // riot api의 riot id 앤드포인트에 접속하기 위한 url( uri 의 한 종류 ) 구성
//                    .fromHttpUrl("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
//                    .queryParam("api_key", riotApiKey) // 인증
//                    .buildAndExpand(gameName.trim(), tagLine.trim())// 대입
//                    .toUri();

            String encodedGameName = UriUtils.encodePathSegment(gameName.trim(), StandardCharsets.UTF_8);
            String encodedTagLine = UriUtils.encodePathSegment(tagLine.trim(), StandardCharsets.UTF_8);

            String riotIdUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                    + encodedGameName + "/" + encodedTagLine + "?api_key=" + riotApiKey;

            URI riotIdUri = URI.create(riotIdUrl);
            System.out.println("[요청 URI] " + riotIdUri);

            // http get 요청
            ResponseEntity<Map> riotIdResponse = restTemplate.getForEntity(riotIdUri, Map.class); // Map.class -> JSON 을 Map 형식으로 변환
            Map<String, Object> riotIdBody = riotIdResponse.getBody(); // body 부분만 꺼내기

            System.out.println("[응답 내용] = " + riotIdBody);

            /* 예시 riot api 응답 구조 ( json )
            {
                "puuid": "o1MvjJkA.. ",
                "gameName": "Hide on Bush",
                "tagLine": "KR1"
            }
             */

            if (riotIdBody == null || riotIdBody.get("puuid") == null) {
                throw new IllegalArgumentException("Riot API 응답이 비어 있거나 잘못됨");
            }

            String puuid = (String) riotIdBody.get("puuid");
            String actualGameName = (String) riotIdBody.get("gameName");
            String actualTagLine = (String) riotIdBody.get("tagLine");

            // puuid -> 소환사 정보
            URI summonerUri = UriComponentsBuilder
                    .fromHttpUrl("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/{puuid}")
                    .queryParam("api_key", riotApiKey)
                    .buildAndExpand(puuid)
                    .toUri();

            ResponseEntity<Map> summonerResponse = restTemplate.getForEntity(summonerUri, Map.class);
            Map<String, Object> summonerBody = summonerResponse.getBody();


            System.out.println("[응답 내용] = " + summonerBody);

            Integer profileIconId = summonerBody.get("profileIconId") != null
                    ? (Integer) summonerBody.get("profileIconId")
                    : null;
            Integer summonerLevel = summonerBody.get("summonerLevel") != null
                    ? ((Number) summonerBody.get("summonerLevel")).intValue()
                    : null;


            // DTO로 리턴
            return SummonerDTO.builder()
                    .puuid(puuid)
                    .gameName(actualGameName)
                    .tagLine(actualTagLine)
                    .profileIconId(profileIconId)
                    .summonerLevel(summonerLevel)
                    .build();

        } catch (Exception e) {
            System.err.println("소환사 조회 오류: " + e.getMessage());
            return null;
        }
    }

    // (*) puuid -> 티어 [ league/v4 ]
    public String getTierByPuuid(String puuid) {
        String url = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid + "?api_key=" + riotApiKey;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<Map<String, Object>> entries = response.getBody();

            for (Map<String, Object> entry : entries) {
                if ("RANKED_SOLO_5x5".equals(entry.get("queueType"))) { // queueType 이 "RANKED_SOLO_5x5 ( 솔로랭크 데이터 ) 인
                    return entry.get("tier") + " " + entry.get("rank");
                }
            }
            return "랭크 없음";
        } catch (Exception e) {
            System.err.println("티어 조회 실패: " + e.getMessage());
            return "에러";
        }
    }

    // 개인 랭크 정보 [ league/v4 ]
    public Map<String, RankDTO> getRankInfoByPuuid(String puuid) {
        String url = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid + "?api_key=" + riotApiKey;

        Map<String, RankDTO> rankMap = new HashMap<>();

        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<Map<String, Object>> body = response.getBody();

            if (body != null) {
                for (Map<String, Object> entry : body) {
                    String queueType = (String) entry.get("queueType");

                    if ("RANKED_SOLO_5x5".equals(queueType) || "RANKED_FLEX_SR".equals(queueType)) {
                        RankDTO dto = new RankDTO();
                        dto.setTier((String) entry.get("tier"));
                        dto.setRank((String) entry.get("rank"));
                        dto.setLeaguePoints(toInt(entry.get("leaguePoints")));
                        dto.setWins(toInt(entry.get("wins")));
                        dto.setLosses(toInt(entry.get("losses")));

                        int wins = dto.getWins();
                        int losses = dto.getLosses();
                        double winRate = (wins + losses > 0) ? (wins * 100.0 / (wins + losses)) : 0.0;
                        dto.setWinRate(winRate);

                        rankMap.put(queueType, dto);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("랭크 정보 조회 실패: " + e.getMessage());
        }

        return rankMap;
    }

}
