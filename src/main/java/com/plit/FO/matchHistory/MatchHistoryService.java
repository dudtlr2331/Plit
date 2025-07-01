package com.plit.FO.matchHistory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    // riotId [ 게임이름( 소환사명 ), 태그( # ) ] -> puuid -> 계정정보 [ account/v1 ]
    public SummonerDTO getAccountByRiotId(String gameName, String tagLine) {

        String encodedGameName = URLEncoder.encode(gameName.trim(), StandardCharsets.UTF_8);
        String encodedTagLine = URLEncoder.encode(tagLine.trim(), StandardCharsets.UTF_8);

        // riotId -> puuid 조회
        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                encodedGameName + "/" + encodedTagLine + "?api_key=" + riotApiKey;

        System.out.println("gameName 원본: [" + gameName + "]");
        System.out.println("tagLine 원본: [" + tagLine + "]");

        System.out.println("최종 Riot API 요청 URL: " + url);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, String> body = response.getBody();

            String puuid = body.get("puuid");
            System.out.println("puuid = " + puuid);

            // puuid -> Summoner API
            String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/"
                    + puuid + "?api_key=" + riotApiKey;

            ResponseEntity<Map> summonerResponse = restTemplate.getForEntity(summonerUrl, Map.class);
            Map<String, Object> summonerBody = summonerResponse.getBody();

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

    // puuid -> 티어 [ league/v4 ]
    public String getTierByPuuid(String puuid) {
        String url = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid + "?api_key=" + riotApiKey;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<Map<String, Object>> entries = response.getBody();

            for (Map<String, Object> entry : entries) {
                if ("RANKED_SOLO_5x5".equals(entry.get("queueType"))) {
                    return entry.get("tier") + " " + entry.get("rank");
                }
            }
            return "랭크 없음";
        } catch (Exception e) {
            System.err.println("티어 조회 실패: " + e.getMessage());
            return "에러";
        }
    }

    // 개인 랭크 정보
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

    // Object 라면 -> int
    private int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // 게임 모드 -> 한글
    private String getQueueType(String code) {
        return switch (code) {
            case "RANKED_SOLO_5x5" -> "솔로랭크";
            case "RANKED_FLEX_SR" -> "자유랭크";
            case "NORMAL" -> "일반";
            case "ARAM" -> "칼바람";
            case "CHERRY", "CHERRY_PICK" -> "특별 모드";
            default -> "기타";
        };
    }

    // 한글 이름으로 불러오기
    private Map<String, String> korNameMap = new HashMap<>();

    // 영어 -> 한글 챔피언 이름
    private String getKorName(String engName) {
        return korNameMap.getOrDefault(engName, engName);
    }

    // 한글 챔피언 이름
    @PostConstruct
    public void loadKorChampionMap() {
        try {
            String url = "https://ddragon.leagueoflegends.com/cdn/14.12.1/data/ko_KR/champion.json";
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            for (String engName : data.keySet()) {
                Map<String, Object> champData = (Map<String, Object>) data.get(engName);
                String korName = (String) champData.get("name");
                korNameMap.put(engName, korName);
            }

            System.out.println("챔피언 한글 이름 불러오기 완료 (" + korNameMap.size() + "개)");
        } catch (Exception e) {
            System.err.println("챔피언 한글 이름 로딩 실패: " + e.getMessage());
        }
    }

    // 선호 5챔피언
    public List<FavoriteChampionDTO> getFavoriteChampions(List<MatchHistoryDTO> matchList) {
        Map<String, List<MatchHistoryDTO>> byChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

        List<FavoriteChampionDTO> result = new ArrayList<>();

        int totalFlexGames = (int) matchList.stream()
                .filter(m -> "RANKED_FLEX_SR".equals(m.getQueueType()))
                .count();

        for (Map.Entry<String, List<MatchHistoryDTO>> entry : byChampion.entrySet()) {
            String engName = entry.getKey();
            List<MatchHistoryDTO> matches = entry.getValue();

            double sumKills = 0, sumDeaths = 0, sumAssists = 0, sumCs = 0, sumCsPerMin = 0;
            int flexGames = 0;

            for (MatchHistoryDTO m : matches) {
                sumKills += m.getKills();
                sumDeaths += m.getDeaths();
                sumAssists += m.getAssists();
                sumCs += m.getCs();
                sumCsPerMin += m.getCsPerMin();

                if ("RANKED_FLEX_SR".equals(m.getQueueType())) {
                    flexGames++;
                }
            }

            int total = matches.size();
            double kdaRatio = sumDeaths == 0 ? sumKills + sumAssists : (sumKills + sumAssists) / sumDeaths;
            String korName = getKorName(engName);

            FavoriteChampionDTO dto = FavoriteChampionDTO.builder()
                    .name(engName)
                    .korName(korName)
                    .kills(sumKills / total)
                    .deaths(sumDeaths / total)
                    .assists(sumAssists / total)
                    .kdaRatio(kdaRatio)
                    .totalCs((int) (sumCs / total))
                    .csPerMin(sumCsPerMin / total)
                    .flexGameCount(flexGames)
                    .flexUsagePercent(totalFlexGames == 0 ? 0 : (flexGames * 100.0 / totalFlexGames))
                    .build();

            result.add(dto);
        }

        // 많이 플레이한 챔피언 순으로 정렬
        result.sort((a, b) -> Integer.compare(
                (int) matchList.stream().filter(m -> m.getChampionName().equals(b.getName())).count(),
                (int) matchList.stream().filter(m -> m.getChampionName().equals(a.getName())).count()
        ));

        // 최대 5개까지만 반환
        return result.size() > 5 ? result.subList(0, 5) : result;
    }


    // 20 게임 통계 요약
    public MatchSummaryDTO getMatchSummary(List<MatchHistoryDTO> matchList) {
        int total = matchList.size();
        if (total == 0) return new MatchSummaryDTO();

        double sumKills = 0, sumDeaths = 0, sumAssists = 0, sumKp = 0;
        int wins = 0;

        // 챔피언
        Map<String, Integer> championTotalGames = new HashMap<>();
        Map<String, Integer> championWins = new HashMap<>();

        // 포지션
        Map<String, Integer> positionTotalGames = new HashMap<>();
        Map<String, Integer> positionWins = new HashMap<>();

        List<String> allPositions = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");
        for (String pos : allPositions) {
            positionTotalGames.put(pos, 0);
            positionWins.put(pos, 0);
        }

        for (MatchHistoryDTO match : matchList) {
            sumKills += match.getKills();
            sumDeaths += match.getDeaths();
            sumAssists += match.getAssists();
            if (match.isWin()) wins++;

            // 킬 관여율
            try {
                sumKp += Double.parseDouble(match.getKillParticipation());
            } catch (Exception ignored) {}

            // 챔피언
            String champ = match.getChampionName();
            championTotalGames.put(champ, championTotalGames.getOrDefault(champ, 0) + 1);
            if (match.isWin()) {
                championWins.put(champ, championWins.getOrDefault(champ, 0) + 1);
            }

            // 포지션
            String pos = match.getTeamPosition();
            positionTotalGames.put(pos, positionTotalGames.getOrDefault(pos, 0) + 1);
            if (match.isWin()) {
                positionWins.put(pos, positionWins.getOrDefault(pos, 0) + 1);
            }
        }

        // 챔피언 승률
        Map<String, Integer> championWinRates = new HashMap<>();
        for (String champ : championTotalGames.keySet()) {
            int count = championTotalGames.get(champ);
            int win = championWins.getOrDefault(champ, 0);
            int rate = (int) Math.round(win * 100.0 / count);
            championWinRates.put(champ, rate);
        }

        // 포지션 승률 (고정 순서 기준)
        Map<String, Integer> positionWinRates = new HashMap<>();
        for (String pos : allPositions) {
            int count = positionTotalGames.getOrDefault(pos, 0);
            int win = positionWins.getOrDefault(pos, 0);
            int rate = (count == 0) ? 0 : (int) Math.round(win * 100.0 / count);
            positionWinRates.put(pos, rate);
        }

        // 정렬 (챔피언은 많이 쓴 순 / 포지션은 고정 X)
        List<Map.Entry<String, Integer>> sortedChampionList = championTotalGames.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .toList();

        int lose = total - wins;

        return MatchSummaryDTO.builder()
                .avgKills(sumKills / total)
                .avgDeaths(sumDeaths / total)
                .avgAssists(sumAssists / total)
                .kdaRatio(sumDeaths == 0 ? (sumKills + sumAssists) : (sumKills + sumAssists) / sumDeaths)
                .killParticipation(sumKp / total)
                .winCount(wins)
                .loseCount(lose)
                .totalCount(total)

                .championTotalGames(championTotalGames)
                .championWins(championWins)
                .championWinRates(championWinRates)
                .sortedChampionList(sortedChampionList)

                .positionTotalGames(positionTotalGames)
                .positionWins(positionWins)
                .positionWinRates(positionWinRates)
                .favoritePositions(new LinkedHashMap<>(positionTotalGames))
                .build();

    }

    // puuid -> matchid <최근 매치 상세 정보> [ match/v5 ]
    public List<MatchHistoryDTO> getMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);
        List<MatchHistoryDTO> result = new ArrayList<>();
        String tier = getTierByPuuid(puuid);

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
                        int teamId = (int) p.get("teamId");

                        // 전체 게임 시간 (초)
                        int durationSeconds = ((Number) info.get("gameDuration")).intValue();

                        // 전체 킬 수 계산
                        int teamTotalKills = participants.stream()
                                .filter(pp -> ((Number) pp.get("teamId")).intValue() == teamId)
                                .mapToInt(pp -> ((Number) pp.get("kills")).intValue())
                                .sum();

                        int kills = ((Number) p.get("kills")).intValue();
                        int assists = ((Number) p.get("assists")).intValue();
                        int deaths = ((Number) p.get("deaths")).intValue();

                        // KDA 계산
                        double kdaRatio = deaths != 0 ? (double) (kills + assists) / deaths : kills + assists;

                        // CS 및 분당 CS
                        int totalMinions = ((Number) p.get("totalMinionsKilled")).intValue()
                                + ((Number) p.get("neutralMinionsKilled")).intValue();
                        double csPerMin = totalMinions / (durationSeconds / 60.0);

                        // 킬관여율
                        double kp = teamTotalKills > 0 ? ((double)(kills + assists) / teamTotalKills) * 100 : 0;

                        // 아이템 아이콘 URL 생성
                        List<String> itemImageUrls = new ArrayList<>();
                        List<String> itemIds = new ArrayList<>();
                        for (int i = 0; i <= 6; i++) {
                            int itemId = (int) p.get("item" + i);
                            itemIds.add(String.valueOf(itemId));
                            if (itemId != 0) {
                                itemImageUrls.add("https://ddragon.leagueoflegends.com/cdn/14.12.1/img/item/" + itemId + ".png");
                            } else {
                                itemImageUrls.add(null);
                            }
                        }

                        MatchHistoryDTO dto = MatchHistoryDTO.builder()
                                .matchId(matchId)
                                .win((Boolean) p.get("win"))
                                .teamPosition((String) p.get("teamPosition"))
                                .championName((String) p.get("championName"))
                                .kills(kills)
                                .deaths(deaths)
                                .assists(assists)
                                .summonerName((String) p.get("summonerName"))
                                .tier(tier)
                                .totalDamageDealtToChampions(((Number) p.get("totalDamageDealtToChampions")).intValue())
                                .totalDamageTaken(((Number) p.get("totalDamageTaken")).intValue())
                                .gameMode((String) info.get("gameMode"))
                                .queueType(String.valueOf(info.get("queueId")))
                                .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                        ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                                .cs(totalMinions)
                                .csPerMin(csPerMin)
                                .killParticipation(String.format("%.1f", kp))
                                .gameDurationMinutes(durationSeconds / 60)
                                .gameDurationSeconds(durationSeconds % 60)
                                .itemIds(itemIds)
                                .itemImageUrls(itemImageUrls)
                                .wardsPlaced(((Number) p.getOrDefault("wardsPlaced", 0)).intValue())
                                .wardsKilled(((Number) p.getOrDefault("wardsKilled", 0)).intValue())
                                .profileIconUrl("https://ddragon.leagueoflegends.com/cdn/14.12.1/img/profileicon/" + p.get("profileIcon") + ".png")
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

    // matchId, puuid <상세 정보 필요할 때 한개만 가져오게! - 데이터 아끼기위해> [ match/v5 ]
    public MatchDetailDTO getMatchDetail(String matchId, String myPuuid) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId + "?api_key=" + riotApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> matchData = response.getBody();
            Map<String, Object> info = (Map<String, Object>) matchData.get("info");
            List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");

            int maxDamage = participants.stream()
                    .mapToInt(p -> (int) p.get("totalDamageDealtToChampions"))
                    .max().orElse(1);

            List<MatchHistoryDTO> blueTeam = new ArrayList<>();
            List<MatchHistoryDTO> redTeam = new ArrayList<>();

            Map<String,String> tierCache = new HashMap<>(); // 데이터 양이 많아 속도가 너무 느려져서

            for (Map<String, Object> p : participants) {
                List<String> itemImageUrls = new ArrayList<>();
                for (int i = 0; i <= 6; i++) {
                    int itemId = (int) p.get("item" + i);
                    if (itemId != 0) {
                        itemImageUrls.add("https://ddragon.leagueoflegends.com/cdn/14.12.1/img/item/" + itemId + ".png");
                    } else {
                        itemImageUrls.add("");
                    }
                }

                String puuid = (String) p.get("puuid");
                String tier = tierCache.get(puuid);
                if (tier == null) {
                    tier = getTierByPuuid(puuid);
                    tierCache.put(puuid, tier);
                }

                MatchHistoryDTO dto = MatchHistoryDTO.builder()
                        .matchId(matchId)
                        .win((Boolean) p.get("win"))
                        .teamPosition((String) p.get("teamPosition"))
                        .championName((String) p.get("championName"))
                        .kills(((Number) p.get("kills")).intValue())
                        .deaths(((Number) p.get("deaths")).intValue())
                        .assists(((Number) p.get("assists")).intValue())
                        .summonerName((String) p.get("summonerName"))
                        .tier(tier)
                        .totalDamageDealtToChampions(((Number) p.get("totalDamageDealtToChampions")).intValue())
                        .totalDamageTaken(((Number) p.get("totalDamageTaken")).intValue())
                        .gameMode((String) info.get("gameMode"))
                        .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                        .profileIconUrl("https://ddragon.leagueoflegends.com/cdn/14.12.1/img/profileicon/" + p.get("profileIcon") + ".png")
                        .itemImageUrls(itemImageUrls)
                        .cs(((Number) p.get("totalMinionsKilled")).intValue()
                                + ((Number) p.get("neutralMinionsKilled")).intValue())
                        .csPerMin(
                                (((Number) p.get("totalMinionsKilled")).intValue()
                                        + ((Number) p.get("neutralMinionsKilled")).intValue())
                                        / (((Number) info.getOrDefault("gameDuration", 1)).doubleValue() / 60.0)
                        )
                        .wardsPlaced(((Number) p.getOrDefault("wardsPlaced", 0)).intValue())
                        .wardsKilled(((Number) p.getOrDefault("wardsKilled", 0)).intValue())
                        .build();

                String teamId = p.get("teamId").toString();
                if (teamId.equals("100")) blueTeam.add(dto);
                else redTeam.add(dto);
            }

            return MatchDetailDTO.builder()
                    .matchId(matchId)
                    .totalMaxDamage(maxDamage)
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .build();

        } catch (Exception e) {
            System.err.println("상세 매치 조회 실패: " + e.getMessage());
            return null;
        }
    }

    // puuid -> 최근 match ID 20개 조회 [ match/v5 ]
    public List<String> getMatchIdsByPuuid(String puuid) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=20&api_key=" + riotApiKey;

            String[] matchIds = restTemplate.getForObject(url, String[].class);
            return Arrays.asList(matchIds);
        } catch (Exception e) {
            System.err.println("매치 ID 조회 실패: " + e.getMessage());
            return List.of();
        }
    }

}