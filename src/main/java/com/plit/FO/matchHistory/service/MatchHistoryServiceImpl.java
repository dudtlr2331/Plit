package com.plit.FO.matchHistory.service;

import com.nimbusds.jose.shaded.gson.Gson;
import com.plit.FO.matchHistory.dto.FavoriteChampionDTO;
import com.plit.FO.matchHistory.dto.MatchSummaryWithListDTO;
import com.plit.FO.matchHistory.dto.SummonerSimpleDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
import com.plit.FO.matchHistory.dto.riot.RiotAccountResponse;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.dto.riot.RiotSummonerResponse;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.*;

@Service
@RequiredArgsConstructor
public class MatchHistoryServiceImpl implements MatchHistoryService { // 매치 정보의 주요 비즈니스 로직

    private final MatchDbService matchDbService;
    private final ImageService imageService;
    private final RiotApiService riotApiService;

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Override
    public List<MatchHistoryDTO> getMatchHistory(String puuid) {
        return List.of();
    }

    // ** Riot id -> puuid ( DB 캐시 활용, riot api 호출 ) **
    @Override
    public String getPuuidOrRequest(String gameName, String tagLine) {
        String normalizedGameName = normalizeGameName(gameName);
        String normalizedTagLine = normalizeTagLine(tagLine);

        // DB 캐시 먼저 조회
        String puuid = matchDbService.findPuuidInCache(normalizedGameName, normalizedTagLine);
        if (puuid != null) return puuid;

        // Riot API 호출
        puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);
        if (puuid != null) {
            matchDbService.saveRiotIdCache(gameName, tagLine, normalizedGameName, normalizedTagLine, puuid);
        }

        return puuid;
    }

    // (*) riotId [ 게임이름( 소환사명 ), 태그( # ) ] -> puuid -> 계정정보 [ account/v1 ]
    public SummonerSimpleDTO getAccountByRiotId(String gameName, String tagLine) {
        try {
            RiotAccountResponse account = riotApiService.getAccountByRiotId(gameName, tagLine);
            if (account == null) return null;

            RiotSummonerResponse summoner = riotApiService.getSummonerByPuuid(account.getPuuid());
            if (summoner == null) return null;

            return SummonerSimpleDTO.builder()
                    .puuid(account.getPuuid())
                    .gameName(account.getGameName())
                    .tagLine(account.getTagLine())
                    .profileIconId(summoner.getProfileIconId())
                    .profileIconUrl(imageService.getProfileIconUrl(summoner.getProfileIconId()))
                    .summonerLevel(summoner.getSummonerLevel())
                    .build();

        } catch (Exception e) {
            System.err.println("소환사 조회 오류: " + e.getMessage());
            return null;
        }
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
    public List<FavoriteChampionDTO> calculateFavoriteChampions(List<MatchHistoryDTO> matchList, String mode, String puuid) {
        Map<String, List<MatchHistoryDTO>> byChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

        System.out.println("[2] calculateFavoriteChampions 호출됨, puuid = " + puuid);

        if (matchList == null || matchList.isEmpty()) {
            return new ArrayList<>();
        }

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
                    .puuid(puuid)
                    .championName(engName)
                    .korName(korName)
                    .kills(round(sumKills / total, 1))
                    .deaths(round(sumDeaths / total, 1))
                    .assists(round(sumAssists / total, 1))
                    .kdaRatio(round(kdaRatio, 1))
                    .averageCs((int) (sumCs / total))
                    .csPerMin(round(sumCsPerMin / total, 1))
                    .flexGames(flexGames)
                    .flexPickRate(totalFlexGames == 0 ? 0 : round(flexGames * 100.0 / totalFlexGames, 1))
                    .championImageUrl(championImageUrl)
                    .gameCount(total)
                    .winCount(wins)
                    .winRate(round(winRate,0))
                    .queueType(mode)
                    .build();

            if ("overall".equals(mode)) {
                dto.setSeasonName("S2025");
            }

            result.add(dto);
        }

        System.out.println("[calculateFavoriteChampions] mode: " + mode + ", result size: " + result.size());
        result.forEach(dto -> System.out.println(" - " + dto.getChampionName() + " (" + dto.getGameCount() + " games)"));

        result.sort(Comparator.comparingInt(FavoriteChampionDTO::getFlexGames).reversed());
        return result;
    }

    // Riot API에서 직접 20경기 가져와서 선호 챔피언 계산 (시즌 필터 없음)
    public List<FavoriteChampionDTO> getFavoriteChampionsFromApi(String puuid, String mode) {
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

        List<MatchHistoryDTO> allMatches = new ArrayList<>();
        for (String matchId : matchIds) {
            RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);
            RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
            if (participant == null) continue;

            int teamId = participant.getTeamId();
            int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

            int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
            double csPerMin = cs / (matchInfo.getGameDurationSeconds() / 60.0);

            LocalDateTime gameEndTime = Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            MatchHistoryDTO dto = MatchHistoryDTO.builder()
                    .matchId(matchId)
                    .championName(participant.getChampionName())
                    .kills(participant.getKills())
                    .deaths(participant.getDeaths())
                    .assists(participant.getAssists())
                    .cs(participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled())
                    .csPerMin(round(csPerMin, 1))
                    .win(participant.isWin())
                    .queueType(matchInfo.getQueueId())
                    .gameEndTimestamp(gameEndTime)
                    .killParticipation(MatchHelper.calculateKillParticipation(
                            participant.getKills(), participant.getAssists(), teamTotalKills
                    ))
                    .build();

            allMatches.add(dto);
        }

        List<MatchHistoryDTO> filtered = allMatches.stream()
                .filter(m -> matchesMode(m, mode))
                .collect(Collectors.toList());

        return calculateFavoriteChampions(filtered, mode, puuid);
    }

    @Override
    public List<FavoriteChampionDTO> getFavoriteChampionsBySeason(String puuid, String season) {
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);
        List<MatchHistoryDTO> allMatches = new ArrayList<>();

        System.out.println("[1] getFavoriteChampionsBySeason 호출됨, puuid = " + puuid);

        for (String matchId : matchIds) {
            RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);

            int queueId;
            try {
                queueId = Integer.parseInt(matchInfo.getQueueId());
            } catch (NumberFormatException e) {
                continue; // queueId가 숫자가 아니면 해당 경기 skip
            }

            if ("solo".equals(season) && queueId != 420) continue;
            if ("flex".equals(season) && queueId != 440) continue;
            if ("overall".equals(season) && queueId != 420 && queueId != 440) continue;

            RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
            if (participant == null) continue;

            int teamId = participant.getTeamId();
            int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

            int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
            double csPerMin = cs / (matchInfo.getGameDurationSeconds() / 60.0);

            LocalDateTime gameEndTime = Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            MatchHistoryDTO dto = MatchHistoryDTO.builder()
                    .matchId(matchId)
                    .championName(participant.getChampionName())
                    .kills(participant.getKills())
                    .deaths(participant.getDeaths())
                    .assists(participant.getAssists())
                    .cs(cs)
                    .csPerMin(round(csPerMin,1))
                    .win(participant.isWin())
                    .queueType(matchInfo.getQueueId())
                    .gameEndTimestamp(gameEndTime)
                    .killParticipation(MatchHelper.calculateKillParticipation(
                            participant.getKills(), participant.getAssists(), teamTotalKills
                    ))
                    .build();

            allMatches.add(dto);
        }

        // 2025년 1월 1일 이후 경기만 필터링
        LocalDateTime seasonStart = LocalDateTime.of(2025, 1, 1, 0, 0);
        List<MatchHistoryDTO> filtered = allMatches.stream()
                .filter(m -> m.getGameEndTimestamp().isAfter(seasonStart))
                .collect(Collectors.toList());

        System.out.println("getFavoriteChampionsBySeason - season: " + season + ", match count after filter: " + filtered.size());

        return calculateFavoriteChampions(filtered, season, puuid);
    }




    public Map<String, List<FavoriteChampionDTO>> getFavoriteChampionsAll(String puuid) {

        System.out.println("== getFavoriteChampionsAll 호출됨 ==");

        Map<String, List<FavoriteChampionDTO>> result = new HashMap<>();

        List<FavoriteChampionDTO> overall = getFavoriteChampionsBySeason(puuid, "overall");
        List<FavoriteChampionDTO> solo = getFavoriteChampionsBySeason(puuid, "solo");
        List<FavoriteChampionDTO> flex = getFavoriteChampionsBySeason(puuid, "flex");

        result.put("overall", overall != null ? overall : new ArrayList<>());
        result.put("solo", solo != null ? solo : new ArrayList<>());
        result.put("flex", flex != null ? flex : new ArrayList<>());

        return result;
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
                    .kdaRatio(round(kdaRatio,1))
                    .averageCs((int) (sumCs / total))
                    .csPerMin(sumCsPerMin / total)
                    .flexGames(flexGames)
                    .flexPickRate(totalFlexGames == 0 ? 0 : (flexGames * 100.0 / totalFlexGames))
                    .championImageUrl(championImageUrl)
                    .build();

            dto.setSeasonName("S2025");

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

    // db 에 정보 없을 때 riot api 에서 직접 matchList 가져옴
    public MatchSummaryDTO getSummaryDirectlyFromApi(String puuid) {
        try {
            // Riot API에서 matchId 20개 받아오기
            List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

            // matchId들로부터 각 게임 정보 받아오기
            List<MatchHistoryDTO> matchList = new ArrayList<>();
            for (String matchId : matchIds) {
                RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);

                // puuid에 해당하는 유저 참가자 정보 뽑아서 MatchHistoryDTO 만들기
                RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
                if (participant == null) continue;

                int teamId = participant.getTeamId();
                int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

                double kp = MatchHelper.calculateKillParticipation(
                        participant.getKills(),
                        participant.getAssists(),
                        teamTotalKills
                );

                MatchHistoryDTO dto = MatchHistoryDTO.builder()
                        .matchId(matchId)
                        .championName(participant.getChampionName())
                        .kills(participant.getKills())
                        .deaths(participant.getDeaths())
                        .assists(participant.getAssists())
                        .win(participant.isWin())
                        .killParticipation(kp)
                        .teamPosition(participant.getTeamPosition())
                        .build();

                matchList.add(dto);
            }

            // 위에서 만든 matchList로 요약 계산
            return getMatchSummary(matchList);

        } catch (Exception e) {
            System.err.println("getSummaryDirectlyFromApi() 중 에러 발생: " + e.getMessage());
            return MatchSummaryDTO.builder().totalCount(0).build();
        }
    }

    public MatchSummaryWithListDTO getSummaryAndListFromApi(String puuid) {
        try {
            RiotAccountResponse account = riotApiService.getAccountByPuuid(puuid);
            String gameName = account.getGameName();
            String tagLine = account.getTagLine();

            List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);
            List<MatchHistoryDTO> matchList = new ArrayList<>();

            for (String matchId : matchIds) {
                RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);
                RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
                if (participant == null) continue;

                int teamId = participant.getTeamId();
                int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);
                double kp = MatchHelper.calculateKillParticipation(participant.getKills(), participant.getAssists(), teamTotalKills);

                int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
                int duration = matchInfo.getGameDurationSeconds();

                MatchHistoryDTO dto = MatchHistoryDTO.builder()
                        .matchId(matchId)
                        .championName(participant.getChampionName())
                        .kills(participant.getKills())
                        .deaths(participant.getDeaths())
                        .assists(participant.getAssists())
                        .win(participant.isWin())
                        .killParticipation(kp)
                        .teamPosition(participant.getTeamPosition())
                        .queueType(matchInfo.getQueueType())
                        .gameMode(matchInfo.getQueueType())
                        .cs(cs)
                        .csPerMin(MatchHelper.getCsPerMin(cs, duration))
                        .build();

                matchList.add(dto);
            }

            MatchSummaryDTO summary = getMatchSummary(matchList);
            MatchOverallSummaryDTO overallSummary = MatchHelper.convertToMatchOverallSummary(
                    puuid, gameName, tagLine, summary, matchList);

            List<FavoriteChampionDTO> favoriteChampions = getFavoriteChampions(matchList);

            return MatchSummaryWithListDTO.builder()
                    .summary(overallSummary)
                    .matchList(matchList)
                    .favoriteChampions(favoriteChampions)
                    .build();

        } catch (Exception e) {
            System.err.println("getSummaryAndListFromApi() 에러: " + e.getMessage());
            return MatchSummaryWithListDTO.builder()
                    .summary(MatchOverallSummaryDTO.builder().totalCount(0).build())
                    .matchList(Collections.emptyList())
                    .favoriteChampions(Collections.emptyList())
                    .build();
        }
    }


    @Override
    public void saveMatchHistory(String puuid) {
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

        for (String matchId : matchIds) {

            // Riot API에서 match 상세 정보 가져오기
            RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);

            MatchDetailDTO detailDTO = new MatchDetailDTO(matchInfo, matchId, puuid);

            String tier ="UNRANKED";
            MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detailDTO, puuid, tier);
            List<MatchPlayerEntity> players = detailDTO.toPlayerEntities();

            // DB 저장
            matchDbService.saveMatchHistory(summary, players);
        }
    }

}
