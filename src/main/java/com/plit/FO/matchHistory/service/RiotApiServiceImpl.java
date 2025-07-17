package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.MatchObjectiveDTO;
import com.plit.FO.matchHistory.dto.RankDTO;
import com.plit.FO.matchHistory.dto.SummonerSimpleDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotAccountResponse;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.dto.riot.RiotSummonerResponse;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.*;

@Service
@RequiredArgsConstructor
public class RiotApiServiceImpl implements RiotApiService{

    private final ImageService imageService;
    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Override
    public List<String> getRecentMatchIds(String puuid, int count) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=" + count + "&api_key=" + riotApiKey;

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[getRecentMatchIds] Error: " + e.getMessage());
            return List.of();
        }
    }



    // riot api 기본정보 가져오기


    @Override
    public RiotAccountResponse getAccountByRiotId(String gameName, String tagLine) {
        try {
            String encodedGameName = UriUtils.encodePathSegment(gameName.trim(), StandardCharsets.UTF_8);
            String encodedTagLine = UriUtils.encodePathSegment(tagLine.trim(), StandardCharsets.UTF_8);

            String riotIdUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                    + encodedGameName + "/" + encodedTagLine + "?api_key=" + riotApiKey;

            URI riotIdUri = URI.create(riotIdUrl);
            ResponseEntity<Map> response = restTemplate.getForEntity(riotIdUri, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || body.get("puuid") == null) return null;

            return RiotAccountResponse.builder()
                    .puuid((String) body.get("puuid"))
                    .gameName((String) body.get("gameName"))
                    .tagLine((String) body.get("tagLine"))
                    .build();

        } catch (Exception e) {
            System.err.println("[Riot API account 오류] " + e.getMessage());
            return null;
        }
    }

    @Override
    public RiotSummonerResponse getSummonerByPuuid(String puuid) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/{puuid}")
                    .queryParam("api_key", riotApiKey)
                    .buildAndExpand(puuid)
                    .toUri();

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) return null;

            return RiotSummonerResponse.builder()
                    .profileIconId((Integer) body.get("profileIconId"))
                    .summonerLevel(((Number) body.get("summonerLevel")).intValue())
                    .build();

        } catch (Exception e) {
            System.err.println("[Riot API summoner 오류] " + e.getMessage());
            return null;
        }
    }



    @Override
    public String requestPuuidFromRiot(String gameName, String tagLine) {
        try {
            URI uri = new URI("https", "asia.api.riotgames.com",
                    "/riot/account/v1/accounts/by-riot-id/" + gameName.trim() + "/" + tagLine.trim(),
                    null);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            return body != null ? (String) body.get("puuid") : null;

        } catch (Exception e) {
            System.err.println("[Riot API 오류] " + e.getMessage());
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

    private RiotParticipantDTO convertToParticipantDTO(Map<String, Object> p) {
        RiotParticipantDTO dto = new RiotParticipantDTO();
        dto.setPuuid((String) p.get("puuid"));
        dto.setSummonerName((String) p.get("summonerName"));
        dto.setChampionName((String) p.get("championName"));
        dto.setTier((String) p.get("tier"));
        dto.setKills(((Number) p.getOrDefault("kills", 0)).intValue());
        dto.setDeaths(((Number) p.getOrDefault("deaths", 0)).intValue());
        dto.setAssists(((Number) p.getOrDefault("assists", 0)).intValue());
        dto.setGoldEarned(((Number) p.getOrDefault("goldEarned", 0)).intValue());
        dto.setTotalDamageDealtToChampions(((Number) p.getOrDefault("totalDamageDealtToChampions", 0)).intValue());
        dto.setTotalDamageTaken(((Number) p.getOrDefault("totalDamageTaken", 0)).intValue());
        dto.setTeamPosition((String) p.get("teamPosition"));
        dto.setWin((Boolean) p.getOrDefault("win", false));
        dto.setTeamId(((Number) p.getOrDefault("teamId", 0)).intValue());
        dto.setChampLevel(((Number) p.getOrDefault("champLevel", 0)).intValue());
        dto.setSummoner1Id(((Number) p.getOrDefault("summoner1Id", 0)).intValue());
        dto.setSummoner2Id(((Number) p.getOrDefault("summoner2Id", 0)).intValue());
        dto.setProfileIcon(((Number) p.getOrDefault("profileIcon", 0)).intValue());
        dto.setTotalMinionsKilled(((Number) p.getOrDefault("totalMinionsKilled", 0)).intValue());
        dto.setNeutralMinionsKilled(((Number) p.getOrDefault("neutralMinionsKilled", 0)).intValue());
        dto.setPerkPrimaryStyle(((Number) p.getOrDefault("perks.styles.0.style", 0)).intValue()); // 주 룬
        dto.setPerkSubStyle(((Number) p.getOrDefault("perks.styles.1.style", 0)).intValue());    // 보조 룬
        dto.setIndividualPosition((String) p.get("individualPosition"));

        // 아이템 ID 모으기
        List<Integer> itemIds = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            String key = "item" + i;
            itemIds.add(((Number) p.getOrDefault(key, 0)).intValue());
        }
        dto.setItemIds(itemIds);

        return dto;
    }

    // (*) puuid -> matchid <최근 매치 정보 - 전적 요약 리스트> [ match/v5 ]
    public List<MatchHistoryDTO> getMatchHistoryFromRiot(String puuid) {
        List<String> matchIds = getRecentMatchIds(puuid, 20);

        System.out.println("[getMatchHistory] puuid = " + puuid);
        System.out.println("[getMatchHistory] matchIds = " + matchIds);

        List<MatchHistoryDTO> result = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                String url = "https://asia.api.riotgames.com/lol/match/v5/matches/"
                        + matchId + "?api_key=" + riotApiKey;

                System.out.println("[getMatchIdsByPuuid] Request URL: " + url);

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> matchData = response.getBody();

                Map<String, Object> metadata = (Map<String, Object>) matchData.get("metadata");
                Map<String, Object> info = (Map<String, Object>) matchData.get("info");

                List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");

                for (Map<String, Object> p : participants) {
                    if (puuid.equals(p.get("puuid"))) {
                        int teamId = (int) p.get("teamId");
                        int durationSeconds = ((Number) info.get("gameDuration")).intValue();
                        int minutes = durationSeconds / 60;
                        int remainSeconds = durationSeconds % 60;


                        int teamTotalKills = participants.stream()
                                .filter(pp -> ((Number) pp.get("teamId")).intValue() == teamId)
                                .mapToInt(pp -> ((Number) pp.get("kills")).intValue())
                                .sum();

                        int kills = ((Number) p.get("kills")).intValue();
                        int assists = ((Number) p.get("assists")).intValue();
                        int deaths = ((Number) p.get("deaths")).intValue();
                        double kdaRatio = MatchHelper.calculateKda(kills, deaths, assists);

                        int totalMinions = ((Number) p.get("totalMinionsKilled")).intValue()
                                + ((Number) p.get("neutralMinionsKilled")).intValue();
                        double csPerMin = totalMinions / (durationSeconds / 60.0);

                        // 킬 관여율
                        double kp = MatchHelper.calculateKillParticipation(kills, assists, teamTotalKills);

                        List<String> itemImageUrls = new ArrayList<>();
                        List<String> itemIds = new ArrayList<>();

                        for (int i = 0; i <= 6; i++) {
                            int itemId = (int) p.get("item" + i);
                            itemIds.add(String.valueOf(itemId));
                            String itemUrl = itemId != 0
                                    ? imageService.getImage(String.valueOf(itemId) + ".png", "item")
                                    .map(ImageEntity::getImageUrl)
                                    .orElse("/images/default.png")
                                    : null;
                            itemImageUrls.add(itemUrl);
                        }

                        List<String> traitImageUrls = new ArrayList<>();
                        for (int i = 1; i <= 4; i++) {
                            Object raw = p.get("playerAugment" + i);
                            if (raw instanceof Integer) {
                                int augmentId = (int) raw;
                                String imageUrl = "/images/trait/" + augmentId + ".png";
                                traitImageUrls.add(imageUrl);
                            }
                        }

                        List<String> otherSummoners = participants.stream()
                                .map(participant -> (String) participant.get("summonerName"))
                                .collect(Collectors.toList());

                        List<String> metadataPuuidList = (List<String>) metadata.get("participants");


                        List<String> otherSummonerNames = new ArrayList<>();
                        List<String> otherProfileIconUrls = new ArrayList<>();

                        for (Map<String, Object> player : participants) {
                            String name = (String) player.get("summonerName");
                            int iconId = (int) player.get("profileIcon");

                            otherSummonerNames.add(name);

                            String iconUrl = imageService.getImage(iconId + ".png", "profile-icon")
                                    .map(ImageEntity::getImageUrl)
                                    .orElse("/images/default.png");

                            otherProfileIconUrls.add(iconUrl);
                        }

                        // imageService 에서 DB 에서 가져온 이미지 경로 매핑
                        String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon") + ".png"), "profile-icon")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/images/default.png");

                        String championImageUrl = imageService.getImage((String) p.get("championName") + ".png", "champion")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/images/default.png");

                        String spell1ImageUrl = imageService.getImage(p.get("summoner1Id") + ".png", "spell")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");
                        String spell2ImageUrl = imageService.getImage(p.get("summoner2Id") + ".png", "spell")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        String mainRune1Url = imageService.getImage(p.get("perkPrimaryStyle") + ".png", "rune")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");
                        String mainRune2Url = imageService.getImage(p.get("perkSubStyle") + ".png", "rune")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        String tier = this.getTierByPuuid(puuid);
                        String tierImageUrl = imageService.getImage(tier + ".png", "tier")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        LocalDateTime endTime = LocalDateTime.ofEpochSecond(
                                ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC);
                        String timeAgo = getTimeAgo(endTime);

                        MatchHistoryDTO dto = MatchHistoryDTO.builder()
                                .matchId(matchId)
                                .win((Boolean) p.get("win"))
                                .teamPosition((String) p.get("teamPosition"))
                                .championName((String) p.get("championName"))
                                .championLevel((int) p.get("champLevel"))
                                .kills(kills)
                                .deaths(deaths)
                                .assists(assists)
                                .kdaRatio(Math.round(kdaRatio * 100) / 100.0)
                                .cs(totalMinions)
                                .csPerMin(Math.round(csPerMin * 10) / 10.0)
                                .killParticipation(Math.round(kp * 10) / 10.0)
                                .gameMode((String) info.get("gameMode"))
                                .queueType(String.valueOf(info.get("queueId")))
                                .gameEndTimestamp(endTime)
                                .gameDurationSeconds(durationSeconds)
                                .timeAgo(timeAgo)
                                .championImageUrl(championImageUrl)
                                .profileIconUrl(profileIconUrl)
                                .itemImageUrls(itemImageUrls)
                                .spell1ImageUrl(spell1ImageUrl)
                                .spell2ImageUrl(spell2ImageUrl)
                                .mainRune1Url(mainRune1Url)
                                .mainRune2Url(mainRune2Url)
                                .tier(tier)
                                .tierImageUrl(tierImageUrl)
                                .traitImageUrls(traitImageUrls)
                                .otherSummonerNames(otherSummonerNames)
                                .otherProfileIconUrls(otherProfileIconUrls)
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

    public RiotMatchInfoDTO getMatchInfo(String matchId) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        Map<String, Object> infoMap = (Map<String, Object>) body.get("info");

        RiotMatchInfoDTO dto = new RiotMatchInfoDTO();

        dto.setGameEndTimestamp(((Number) infoMap.get("gameEndTimestamp")).longValue());
        dto.setGameDurationSeconds(((Number) infoMap.get("gameDuration")).intValue());
        dto.setGameMode((String) infoMap.get("gameMode"));
        dto.setQueueId(String.valueOf(infoMap.get("queueId")));

        List<Map<String, Object>> participantsMap = (List<Map<String, Object>>) infoMap.get("participants");
        List<RiotParticipantDTO> participants = participantsMap.stream()
                .map(p -> convertToParticipantDTO(p))
                .collect(Collectors.toList());
        dto.setParticipants(participants);

        return dto;
    }

    @Override
    public MatchDetailDTO getMatchDetailFromRiot(String matchId, String puuid) {
        RiotMatchInfoDTO matchInfo = getMatchInfo(matchId); // Riot API로 match info 가져오기
        List<RiotParticipantDTO> participants = matchInfo.getParticipants();

        long timestamp = matchInfo.getGameEndTimestamp();
        LocalDateTime gameEndTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();

        return MatchDetailDTO.builder()
                .matchId(matchId)
                .gameMode(matchInfo.getGameMode())
                .gameEndTimestamp(gameEndTime)
                .participants(participants)
                .build();
    }


}
