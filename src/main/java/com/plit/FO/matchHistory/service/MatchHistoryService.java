package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MatchHistoryService {

    private final RestTemplate restTemplate;
    private final ImageService imageService;

    private final RiotIdCacheRepository riotIdCacheRepository;

    @Value("${riot.api.key}")
    private String riotApiKey;


//    riot api 기본정보 가져오기

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


//    왼쪽 파넬

    // 최근 시즌 시작일 자동 추정 (랭크 매치 중 가장 오래된 날짜)
    private LocalDateTime getSeasonStart(List<MatchHistoryDTO> matchList) {
        return matchList.stream()
                .filter(m -> "420".equals(m.getQueueType()) || "440".equals(m.getQueueType()))
                .map(MatchHistoryDTO::getGameEndTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusMonths(3)); // fallback: 최근 3개월
    }

    // 시즌 필터 (동적으로 시즌 시작일 넘겨받음)
    private boolean isCurrentSeason(MatchHistoryDTO m, LocalDateTime seasonStart) {
        return m.getGameEndTimestamp().isAfter(seasonStart);
    }

    // 모드 필터 (솔로/자유/전체)
    private boolean matchesMode(MatchHistoryDTO m, String mode) {
        if ("solo".equals(mode)) return "420".equals(m.getQueueType());
        if ("flex".equals(mode)) return "440".equals(m.getQueueType());
        return "420".equals(m.getQueueType()) || "440".equals(m.getQueueType());
    }

    // 내부 계산 로직
    private List<FavoriteChampionDTO> calculateFavoriteChampions(List<MatchHistoryDTO> matchList, String mode) {
        Map<String, List<MatchHistoryDTO>> byChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

        int totalFlexGames = (int) matchList.stream()
                .filter(m -> "440".equals(m.getQueueType()))
                .count();

        List<FavoriteChampionDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<MatchHistoryDTO>> entry : byChampion.entrySet()) {
            String engName = entry.getKey();
            List<MatchHistoryDTO> matches = entry.getValue();

            double sumKills = 0, sumDeaths = 0, sumAssists = 0, sumCs = 0, sumCsPerMin = 0;
            int flexGames = 0, soloGames = 0;

            for (MatchHistoryDTO m : matches) {
                sumKills += m.getKills();
                sumDeaths += m.getDeaths();
                sumAssists += m.getAssists();
                sumCs += m.getCs();
                sumCsPerMin += m.getCsPerMin();

                if ("440".equals(m.getQueueType())) flexGames++;
                if ("420".equals(m.getQueueType())) soloGames++;
            }

            if ("flex".equals(mode) && flexGames == 0) continue;
            if ("solo".equals(mode) && soloGames == 0) continue;

            int total = matches.size();
            int wins = (int) matches.stream().filter(MatchHistoryDTO::isWin).count();
            int winRate = total == 0 ? 0 : (int) round(wins * 100.0 / total, 0);
            double kdaRatio = sumDeaths == 0 ? sumKills + sumAssists : (sumKills + sumAssists) / sumDeaths;
            String korName = getKorName(engName);

            String championImageUrl = imageService.getImage(engName + ".png", "champion")
                    .map(ImageEntity::getImageUrl)
                    .orElse("/images/default.png");

            FavoriteChampionDTO dto = FavoriteChampionDTO.builder()
                    .championName(engName)
                    .korName(korName)
                    .kills(sumKills / total)
                    .deaths(sumDeaths / total)
                    .assists(sumAssists / total)
                    .kdaRatio(round(kdaRatio, 2))
                    .averageCs((int) (sumCs / total))
                    .csPerMin(round(sumCsPerMin / total, 1))
                    .flexGames(flexGames)
                    .flexPickRate(totalFlexGames == 0 ? 0 : round(flexGames * 100.0 / totalFlexGames, 1))
                    .championImageUrl(championImageUrl)
                    .gameCount(total)
                    .winCount(wins)
                    .winRate(winRate)
                    .build();

            result.add(dto);
        }

        result.sort(Comparator.comparingInt(FavoriteChampionDTO::getFlexGames).reversed());
        return result;
    }

    // 시즌 기반 선호 챔피언
    public List<FavoriteChampionDTO> getFavoriteChampionsBySeason(String puuid, String mode) {
        List<MatchHistoryDTO> allMatches = getMatchHistory(puuid);

        LocalDateTime seasonStart = getSeasonStart(allMatches);
        List<MatchHistoryDTO> filtered = allMatches.stream()
                .filter(m -> isCurrentSeason(m, seasonStart))
                .filter(m -> matchesMode(m, mode))
                .collect(Collectors.toList());

        return calculateFavoriteChampions(filtered, mode);
    }


//    오른쪽 파넬

    // 선호 5챔피언 ( 20게임 각각의 전적을 기반으로 => MatchHistoryDTO )
    public List<FavoriteChampionDTO> getFavoriteChampions(List<MatchHistoryDTO> matchList) {
        Map<String, List<MatchHistoryDTO>> byChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

        List<FavoriteChampionDTO> result = new ArrayList<>();

        // 자유랭크 게임인 큐 타입 필터링 개수
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

            String championImageUrl = imageService.getImage(engName + ".png", "champion")
                    .map(ImageEntity::getImageUrl)
                    .orElse("/img/default.png");


            FavoriteChampionDTO dto = FavoriteChampionDTO.builder()
                    .championName(engName)
                    .korName(korName)
                    .kills(sumKills / total)
                    .deaths(sumDeaths / total)
                    .assists(sumAssists / total)
                    .kdaRatio(kdaRatio)
                    .averageCs((int) (sumCs / total))
                    .csPerMin(sumCsPerMin / total)
                    .flexGames(flexGames)
                    .flexPickRate(totalFlexGames == 0 ? 0 : (flexGames * 100.0 / totalFlexGames))
                    .championImageUrl(championImageUrl)
                    .build();

            result.add(dto);
        }

        // 많이 플레이한 챔피언 순으로 정렬
        result.sort((a, b) -> Integer.compare(
                (int) matchList.stream().filter(m -> m.getChampionName().equals(b.getChampionName())).count(),
                (int) matchList.stream().filter(m -> m.getChampionName().equals(a.getChampionName())).count()
        ));

        // 최대 5개까지만 반환
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    // (*) 전체 20 게임 통계 요약
    public MatchSummaryDTO getMatchSummary(List<MatchHistoryDTO> matchList) {
        int total = matchList.size();
        System.out.println("getMatchSummary() 호출됨 matchList size = " + matchList.size());


        if (total == 0) {
            System.out.println("matchList가 비어있어서 요약 통계를 만들 수 없음");
            return MatchSummaryDTO.builder()
                    .avgKills(0)
                    .avgDeaths(0)
                    .avgAssists(0)
                    .kdaRatio(0.0)
                    .killParticipation(0.0)
                    .winCount(0)
                    .loseCount(0)
                    .totalCount(0)

                    .championTotalGames(new HashMap<>())
                    .championWins(new HashMap<>())
                    .championWinRates(new HashMap<>())
                    .sortedChampionList(new ArrayList<>())

                    .positionTotalGames(new HashMap<>())
                    .positionWins(new HashMap<>())
                    .positionWinRates(new HashMap<>())
                    .favoritePositions(new LinkedHashMap<>())
                    .sortedPositionList(List.of("TOP", "JUNGLE", "MID", "BOTTOM", "UTILITY"))
                    .build();
        }


        double sumKills = 0, sumDeaths = 0, sumAssists = 0, sumKp = 0;
        int wins = 0;

        // 챔피언
        Map<String, Integer> championTotalGames = new HashMap<>();
        Map<String, Integer> championWins = new HashMap<>();


        // 포지션
        Map<String, Integer> positionTotalGames = new HashMap<>();
        Map<String, Integer> positionWins = new HashMap<>();

        List<String> allPositions = List.of("TOP", "JUNGLE", "MID", "BOTTOM", "UTILITY");
        for (String pos : allPositions) {
            positionTotalGames.put(pos, 0);
            positionWins.put(pos, 0);
        }

        for (MatchHistoryDTO match : matchList) {
            sumKills += match.getKills();
            sumDeaths += match.getDeaths();
            sumAssists += match.getAssists();
            if (match.isWin()) wins++;

            // 킬 관여율 누적
            try {
                sumKp += Optional.ofNullable(match.getKillParticipation()).orElse(0.0);
            } catch (NumberFormatException ignored) {}

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
            championWins.putIfAbsent(champ, 0);
            int count = championTotalGames.get(champ);
            int win = championWins.get(champ);
            int rate = (int) round(win * 100.0 / count, 0);
            championWinRates.put(champ, rate);
        }

        // 포지션 승률 (고정 순서 기준)
        Map<String, Integer> positionWinRates = new HashMap<>();
        for (String pos : allPositions) {
            int count = positionTotalGames.getOrDefault(pos, 0);
            int win = positionWins.getOrDefault(pos, 0);
            int rate = (count == 0) ? 0 : (int) round(win * 100.0 / count, 0);
            positionWinRates.put(pos, rate);
        }

        // 선호 챔피언
        Map<String, Integer> favoritePositions = new HashMap<>();
        for (MatchHistoryDTO match : matchList) {
            String position = match.getTeamPosition();
            if (position != null && !position.isBlank()) {
                favoritePositions.put(position, favoritePositions.getOrDefault(position, 0) + 1);
            }
        }

        // 정렬 (챔피언은 많이 쓴 순 / 포지션은 고정 X)
        List<Map.Entry<String, Integer>> sortedChampionList = championTotalGames.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .toList();

        int lose = total - wins;

        List<String> sortedPositionList = favoritePositions.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        System.out.println("[정렬된 챔피언 리스트] = " + sortedChampionList);


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
                .favoritePositions(favoritePositions)
                .sortedPositionList(sortedPositionList)
                .build();

    }

    // (*) puuid -> matchid <최근 매치 정보 - 전적 요약 리스트> [ match/v5 ]
    public List<MatchHistoryDTO> getMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);

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
                        int durationMinutes = durationSeconds / 60;
                        int durationRemainSeconds = durationSeconds % 60;

                        int teamTotalKills = participants.stream()
                                .filter(pp -> ((Number) pp.get("teamId")).intValue() == teamId)
                                .mapToInt(pp -> ((Number) pp.get("kills")).intValue())
                                .sum();

                        int kills = ((Number) p.get("kills")).intValue();
                        int assists = ((Number) p.get("assists")).intValue();
                        int deaths = ((Number) p.get("deaths")).intValue();
                        double kdaRatio = deaths != 0 ? (double) (kills + assists) / deaths : kills + assists;

                        int totalMinions = ((Number) p.get("totalMinionsKilled")).intValue()
                                + ((Number) p.get("neutralMinionsKilled")).intValue();
                        double csPerMin = totalMinions / (durationSeconds / 60.0);

                        double kp = teamTotalKills > 0 ? ((double)(kills + assists) / teamTotalKills) * 100 : 0;

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

                        String tier = getTierByPuuid(puuid);
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
                                .gameDurationMinutes(durationMinutes)
                                .gameDurationSeconds(durationRemainSeconds)
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

    // matchId, puuid <상세 정보 필요할 때 한게임씩 가져오게! - 데이터 아끼기위해> [ match/v5 ]
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

            List<MatchPlayerDTO> blueTeam = new ArrayList<>();
            List<MatchPlayerDTO> redTeam = new ArrayList<>();

            Map<String,String> tierCache = new HashMap<>();

            for (Map<String, Object> p : participants) {
                List<String> itemImageUrls = new ArrayList<>();
                for (int i = 0; i <= 6; i++) {
                    int itemId = (int) p.get("item" + i);
                    String itemUrl = itemId != 0
                            ? imageService.getImage(String.valueOf(itemId) + ".png", "item")
                            .map(ImageEntity::getImageUrl)
                            .orElse("")
                            : null;
                    itemImageUrls.add(itemUrl);
                }

                String puuid = (String) p.get("puuid");
                int kills = ((Number) p.get("kills")).intValue();
                int deaths = ((Number) p.get("deaths")).intValue();
                int assists = ((Number) p.get("assists")).intValue();

                double kdaRatio = deaths != 0 ? (double)(kills + assists) / deaths : kills + assists;

                String tier = tierCache.computeIfAbsent(puuid, k -> getTierByPuuid(k));
                String tierImageUrl = imageService.getImage(tier + ".png", "tier")
                        .map(ImageEntity::getImageUrl)
                        .orElse("");


                Map<String, Object> perks = (Map<String, Object>) p.get("perks");
                List<Map<String, Object>> styles = (List<Map<String, Object>>) perks.get("styles");

                int mainRune1 = (int) ((Map<String, Object>) ((List<?>) styles.get(0).get("selections")).get(0)).get("perk");
                int mainRune2 = (int) styles.get(1).get("style");

                Map<String, Object> statPerks = (Map<String, Object>) perks.get("statPerks");
                int statRune1 = (int) statPerks.get("offense");
                int statRune2 = (int) statPerks.get("flex");

                int cs = ((Number) p.get("totalMinionsKilled")).intValue()
                        + ((Number) p.get("neutralMinionsKilled")).intValue();
                double csPerMin = cs / (((Number) info.getOrDefault("gameDuration", 1)).doubleValue() / 60.0);


                String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon") + ".png"), "profile-icon")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String championImageUrl = imageService.getImage((String) p.get("championName") + ".png", "champion")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String mainRune1Url = imageService.getImage(String.valueOf(mainRune1) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String mainRune2Url = imageService.getImage(String.valueOf(mainRune2) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String statRune1Url = imageService.getImage(String.valueOf(statRune1) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String statRune2Url = imageService.getImage(String.valueOf(statRune2) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String teamPosition = normalizePosition((String) p.get("teamPosition"));

                MatchPlayerDTO dto = MatchPlayerDTO.builder()
                        .matchId(matchId)
                        .win((Boolean) p.get("win"))
                        .teamPosition(teamPosition)
                        .championName((String) p.get("championName"))
                        .kills(((Number) p.get("kills")).intValue())
                        .deaths(((Number) p.get("deaths")).intValue())
                        .assists(((Number) p.get("assists")).intValue())
                        .summonerName((String) p.get("summonerName"))
                        .kdaRatio(Math.round(kdaRatio * 100) / 100.0)
                        .tier(tier)
                        .tierImageUrl(tierImageUrl)
                        .totalDamageDealtToChampions(((Number) p.get("totalDamageDealtToChampions")).intValue())
                        .totalDamageTaken(((Number) p.get("totalDamageTaken")).intValue())
                        .gameMode((String) info.get("gameMode"))
                        .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                        .profileIconUrl(profileIconUrl)
                        .championImageUrl(championImageUrl)
                        .mainRune1(mainRune1)
                        .mainRune2(mainRune2)
                        .statRune1(statRune1)
                        .statRune2(statRune2)
                        .mainRune1Url(mainRune1Url)
                        .mainRune2Url(mainRune2Url)
                        .statRune1Url(statRune1Url)
                        .statRune2Url(statRune2Url)
                        .itemImageUrls(itemImageUrls)
                        .cs(cs)
                        .csPerMin(csPerMin)
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

    // Riot id -> puuid ( DB 캐시 활용, riot api 호출 )
    public String getPuuidOrRequest(String gameName, String tagLine) {
        // 소문자화 + 띄어쓰기 제거
        String normalizedGameName = normalizeGameName(gameName);
        String normalizedTagLine = normalizeTagLine(tagLine);

        // 정규화된 값으로 캐시 DB 먼저 조회
        Optional<RiotIdCacheEntity> cache = riotIdCacheRepository
                .findByNormalizedGameNameAndNormalizedTagLine(normalizedGameName, normalizedTagLine);

        if (cache.isPresent()) {
            System.out.println("[캐시 HIT] " + gameName + "#" + tagLine + " -> " + cache.get().getPuuid());
            return cache.get().getPuuid();
        }

        // 캐시에 없으면 Riot API에 정확한 원본 값으로 요청
        try { // UriUtils.encodePathsegment() : URL에 넣을 수 있도록 문자열을 안전하게 변환(인코딩)해주는 메서드
            String encodedGameName = UriUtils.encodePathSegment(gameName.trim(), StandardCharsets.UTF_8);
            String encodedTagLine = UriUtils.encodePathSegment(tagLine.trim(), StandardCharsets.UTF_8);

            String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                    encodedGameName + "/" + encodedTagLine;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();

            if (body != null && body.get("puuid") != null) {
                String puuid = (String) body.get("puuid");

                // DB 저장 (원본 + 정규화된 값 모두 저장)
                RiotIdCacheEntity saved = RiotIdCacheEntity.builder()
                        .gameName(gameName.trim())
                        .tagLine(tagLine.trim())
                        .normalizedGameName(normalizedGameName)
                        .normalizedTagLine(normalizedTagLine)
                        .puuid(puuid)
                        .build();

                riotIdCacheRepository.save(saved);

                return puuid;
            }

        } catch (Exception e) {
            System.err.println("[Riot API 오류] " + e.getMessage());
        }

        return null;
    }



    // 소문자화 + 띄어쓰기 제거
    public String normalizeGameName(String gameName) {
        return gameName.trim().replaceAll("\\s+", "").toLowerCase();
    }
    public String normalizeTagLine(String tagLine) {
        return tagLine.trim().toLowerCase();
    }
    private String normalizePosition(String pos) {
        if (pos == null) return "unknown";
        return switch (pos.toUpperCase()) {
            case "TOP" -> "top";
            case "JUNGLE" -> "jungle";
            case "MID", "MIDDLE" -> "mid";
            case "ADC", "BOTTOM", "BOT" -> "bottom";
            case "SUPPORT", "UTILITY" -> "support";
            default -> "unknown";
        };
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

    // 반올림
    private double round(double value, int precision) {
        return Math.round(value * Math.pow(10, precision)) / Math.pow(10, precision);
    }

    // 몇 일, 몇 시간, 몇 분 전 매치였는지
    private String getTimeAgo(LocalDateTime gameEndTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(gameEndTime, now);

        long days = duration.toDays();
        long hours = duration.toHours();
        long minutes = duration.toMinutes();

        if (days > 0) {
            return days + "일 전";
        } else if (hours > 0) {
            return hours + "시간 전";
        } else if (minutes > 0) {
            return minutes + "분 전";
        } else {
            return "방금 전";
        }
    }



    // 한글 이름으로 불러오기
    private Map<String, String> korNameMap = new HashMap<>();

    // 한글 챔피언 이름 - riot 챔피언 json 으로 호출 - 모든 챔피언의 영어 이름(key) 과 한글 이름 필드가 들어있음
    @PostConstruct
    public void loadKorChampionMap() {
        try {
            String url = "https://ddragon.leagueoflegends.com/cdn/14.12.1/data/ko_KR/champion.json";
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            // korNameMap 에 매핑 저장
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

    // 영어 -> 한글 챔피언 이름
    private String getKorName(String engName) {
        return korNameMap.getOrDefault(engName, engName);
    }

}