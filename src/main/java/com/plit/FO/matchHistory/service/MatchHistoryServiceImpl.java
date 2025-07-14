package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.FavoriteChampionDTO;
import com.plit.FO.matchHistory.dto.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.MatchPlayerRepository;
import com.plit.FO.matchHistory.repository.MatchSummaryRepository;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.*;

@Service
@RequiredArgsConstructor
public class MatchHistoryServiceImpl implements MatchHistoryService { // 매치 정보의 주요 비즈니스 로직

    private final MatchDbService matchDbService;
    private final RiotIdCacheRepository riotIdCacheRepository;
    private final ImageService imageService;

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Override
    public List<MatchHistoryDTO> getMatchHistory(String puuid) {
        return List.of();
    }

    @Override
    public MatchDetailDTO getMatchDetail(String matchId) {
        return null;
    }

    // 왼쪽 패널

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

        List<MatchHistoryDTO> allMatches = matchDbService.getMatchHistoryFromRiot(puuid);

        LocalDateTime seasonStart = getSeasonStart(allMatches);
        List<MatchHistoryDTO> filtered = allMatches.stream()
                .filter(m -> isCurrentSeason(m, seasonStart))
                .filter(m -> matchesMode(m, mode))
                .collect(Collectors.toList());

        return calculateFavoriteChampions(filtered, mode);
    }



    // 오른쪽 패널

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
                    .orElse("/images/default.png");


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



}
