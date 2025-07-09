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
    private final ImageService imageService;

    private final RiotIdCacheRepository riotIdCacheRepository;

    @Value("${riot.api.key}")
    private String riotApiKey;


    // (*) riotId [ 게임이름( 소환사명 ), 태그( # ) ] -> puuid -> 계정정보 [ account/v1 ]
    public SummonerDTO getAccountByRiotId(String gameName, String tagLine) {
        try {
            // Riot ID -> puuid, gameName, tagLine // uri( 자원 식별 방식 ) 만들기
            URI riotIdUri = UriComponentsBuilder // URI 생성도구
                    // riot api의 riot id 앤드포인트에 접속하기 위한 url( uri 의 한 종류 ) 구성
                    .fromHttpUrl("https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                    .queryParam("api_key", riotApiKey) // 인증
                    .buildAndExpand(gameName.trim(), tagLine.trim())// 대입
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            // http get 요청
            ResponseEntity<Map> riotIdResponse = restTemplate.getForEntity(riotIdUri, Map.class); // Map.class -> JSON 을 Map 형식으로 변환
            Map<String, Object> riotIdBody = riotIdResponse.getBody(); // body 부분만 꺼내기

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

            Integer profileIconId = summonerBody.get("profileIconId") != null
                    ? (Integer) summonerBody.get("profileIconId")
                    : null;

            // DTO로 리턴
            return SummonerDTO.builder()
                    .puuid(puuid)
                    .gameName(actualGameName)
                    .tagLine(actualTagLine)
                    .profileIconId(profileIconId)
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
        if (total == 0) return new MatchSummaryDTO();

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
                .sortedPositionList(allPositions)
                .build();

    }

    // (*) puuid -> matchid <최근 매치 정보 - 전적 요약 리스트> [ match/v5 ]
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
                        int durationSeconds = ((Number) info.get("gameDuration")).intValue();

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
                                    ? imageService.getImage(String.valueOf(itemId), "item")
                                    .map(ImageEntity::getImageUrl)
                                    .orElse("/img/default.png")
                                    : null;
                            itemImageUrls.add(itemUrl);
                        }

                        // imageService 에서 DB 에서 가져온 이미지 경로 매핑
                        String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon")), "profile-icon")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/img/default.png");

                        String championImageUrl = imageService.getImage((String) p.get("championName"), "champion")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/img/default.png");

                        MatchHistoryDTO dto = MatchHistoryDTO.builder()
                                .matchId(matchId)
                                .win((Boolean) p.get("win"))
                                .teamPosition((String) p.get("teamPosition"))
                                .championName((String) p.get("championName"))
                                .kills(kills)
                                .deaths(deaths)
                                .assists(assists)
                                .kdaRatio(kdaRatio)
                                .csPerMin(csPerMin)
                                .killParticipation(kp)
                                .gameMode((String) info.get("gameMode"))
                                .queueType(String.valueOf(info.get("queueId")))
                                .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                        ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                                .championImageUrl(championImageUrl)
                                .profileIconUrl(profileIconUrl)
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
                            ? imageService.getImage(String.valueOf(itemId), "item")
                            .map(ImageEntity::getImageUrl)
                            .orElse("/img/default.png")
                            : null;
                    itemImageUrls.add(itemUrl);
                }

                String puuid = (String) p.get("puuid");
                String tier = tierCache.computeIfAbsent(puuid, k -> getTierByPuuid(k));

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


                String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon")), "profile-icon")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

                String championImageUrl = imageService.getImage((String) p.get("championName"), "champion")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

                String mainRune1Url = imageService.getImage(String.valueOf(mainRune1), "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

                String mainRune2Url = imageService.getImage(String.valueOf(mainRune2), "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

                String statRune1Url = imageService.getImage(String.valueOf(statRune1), "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

                String statRune2Url = imageService.getImage(String.valueOf(statRune2), "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/img/default.png");

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
                        .tier(tier)
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
    private String normalizeGameName(String gameName) {
        return gameName.trim().replaceAll("\\s+", "").toLowerCase();
    }
    private String normalizeTagLine(String tagLine) {
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

}