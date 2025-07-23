package com.plit.FO.matchHistory.service;

import com.nimbusds.jose.shaded.gson.Gson;
import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.*;
import com.plit.FO.matchHistory.dto.riot.RiotAccountResponse;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.*;
import com.plit.FO.matchHistory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDbServiceImpl implements MatchDbService{ // 전적 검색 DB 저장, 조회

    @Override
    public void fetchAndSaveAllIfNotExists(String puuid, String tagLine) {

    }

    private final MatchSummaryRepository matchSummaryRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final RiotApiService riotApiService;
    private final RiotIdCacheRepository riotIdCacheRepository;
    private final ImageService imageService;

    private final FavoriteChampionRepository favoriteChampionRepository;
    private final MatchOverallSummaryRepository matchOverallSummaryRepository;

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Override
    public String findPuuidInCache(String normalizedGameName, String normalizedTagLine) {
        return riotIdCacheRepository
                .findByNormalizedGameNameAndNormalizedTagLine(normalizedGameName, normalizedTagLine)
                .map(RiotIdCacheEntity::getPuuid)
                .orElse(null);
    }

    @Override
    public void saveRiotIdCache(String gameName, String tagLine, String normalizedGameName, String normalizedTagLine, String puuid) {
        RiotIdCacheEntity entity = RiotIdCacheEntity.builder()
                .gameName(gameName.trim())
                .tagLine(tagLine.trim())
                .normalizedGameName(normalizedGameName)
                .normalizedTagLine(normalizedTagLine)
                .puuid(puuid)
                .build();
        riotIdCacheRepository.save(entity);
    }


    // puuid -> 최근 match ID 조회 [ match/v5 ]
    public List<String> getRecentMatchIds(String puuid, int count) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=" + count + "&api_key=" + riotApiKey;

            String[] matchIds = restTemplate.getForObject(url, String[].class);
            return Arrays.asList(matchIds);
        } catch (Exception e) {
            System.err.println("매치 ID 조회 실패: " + e.getMessage());
            return List.of();
        }
    }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    public void initMatchHistory(String puuid) {
        // 최근 match ID 20개 가져오기
        List<String> matchIds = getRecentMatchIds(puuid, 20);

        for (String matchId : matchIds) {
            // 이미 저장된 matchId는 건너뛰기
            if (matchSummaryRepository.existsByMatchId(matchId)) {
                continue;
            }

            // Riot API로 match 상세 정보 가져오기
            RiotMatchInfoDTO info = riotApiService.getMatchInfo(matchId);
            List<RiotParticipantDTO> participants = info.getParticipants();

            // 본인 participant만 추출
            RiotParticipantDTO me = participants.stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .orElse(null);
            if (me == null) continue;

            LocalDateTime endTime = LocalDateTime.ofEpochSecond(info.getGameEndTimestamp() / 1000, 0, ZoneOffset.UTC);

            // 요약 정보 생성
            MatchSummaryEntity summary = MatchSummaryEntity.builder()
                    .matchId(matchId)
                    .puuid(puuid)
                    .win(me.isWin())
                    .teamPosition(me.getTeamPosition())
                    .championName(me.getChampionName())
                    .kills(me.getKills())
                    .deaths(me.getDeaths())
                    .assists(me.getAssists())
                    .kdaRatio(getKda(me.getKills(), me.getDeaths(), me.getAssists()))
                    .tier(riotApiService.getTierByPuuid(puuid))
                    .gameEndTimestamp(endTime)
                    .gameMode(info.getGameMode())
                    .cs(me.getTotalMinionsKilled())
                    .itemIds("")
                    .createdAt(null)
                    .build();

            String queueType = info.getQueueId();



            // 상세 정보 리스트 생성
            List<MatchPlayerEntity> players = participants.stream()
                    .map(p -> MatchPlayerEntity.builder()
                            .matchId(matchId)
                            .puuid(p.getPuuid())
                            .summonerName(p.getSummonerName())
                            .championName(p.getChampionName())
                            .kills(p.getKills())
                            .deaths(p.getDeaths())
                            .assists(p.getAssists())
                            .kdaRatio(getKda(p.getKills(), p.getDeaths(), p.getAssists()))
                            .cs(0) // CS는 없는 경우 0으로
                            .csPerMin(0)
                            .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                            .totalDamageTaken(p.getTotalDamageTaken())
                            .teamPosition(p.getTeamPosition())
                            .mainRune1(0) // 추후 룬 파싱 가능하면 반영
                            .mainRune2(0)
                            .statRune1(0)
                            .statRune2(0)
                            .wardsPlaced(0)
                            .wardsKilled(0)
                            .gameEndTimestamp(toLocalDateTime(info.getGameEndTimestamp()))
                            .gameMode(info.getGameMode())
                            .queueType(queueType)
                            .teamId(p.getTeamId())
                            .win(p.isWin())
                            .itemIds("")
                            .goldEarned(p.getGoldEarned())
                            .build()
                    )
                    .collect(Collectors.toList());

            // 저장
            saveMatchHistory(summary, players);
        }
    }


    @Override
    public List<String> getMatchIdsByPuuid(String puuid) {
        return List.of();
    }

    public void updateOverallSummary(String puuid, String gameName, String tagLine, String tier) {
        // 해당 유저의 모든 전적 요약 가져오기
        List<MatchSummaryEntity> matchList = matchSummaryRepository.findByPuuid(puuid);

        // 요약 통계 계산
        MatchOverallSummaryDTO dto = MatchHelper.getOverallSummary(puuid, gameName, tagLine, matchList);
        dto.setTier(tier);
        log.info("[updateOverallSummary] 요약 계산 결과 dto={}", dto);

        // 포지션 이미지 설정
        dto.setPreferredPositionImageUrl(imageService.getImageUrl(dto.getPreferredPosition() + ".svg", "position"));

        // 선호 챔피언 이미지 설정
        List<String> championImageUrls = dto.getPreferredChampions().stream()
                .map(championName -> imageService.getImageUrl(championName + ".png", "champion"))
                .toList();
        dto.setFavoriteChampionImageUrls(championImageUrls);

        Optional<MatchOverallSummaryEntity> existing = matchOverallSummaryRepository.findByPuuid(puuid);

        // DTO -> Entity 변환
        MatchOverallSummaryEntity entity = MatchOverallSummaryEntity.fromDTO(dto);

        if (existing.isPresent()) {
            entity.setId(existing.get().getId());
            log.info("[updateOverallSummary] 기존 엔티티 덮어씀, id={}", entity.getId());
        } else {
            log.info("[updateOverallSummary] 새로운 엔티티 저장");
        }

        // 저장
        matchOverallSummaryRepository.save(entity);
        log.info("match_overall_summary 저장 완료 for puuid={}", puuid);
    }

    // dto -> entity -> db ( 내부 계산 로직 )
    public List<FavoriteChampionDTO> calculateFavoriteChampions(List<MatchHistoryDTO> matchList, String mode, String puuid) {
        Map<String, List<MatchHistoryDTO>> byChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

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
                    .orElse("/images/riot_default.png");

            FavoriteChampionDTO dto = FavoriteChampionDTO.builder()
                    .puuid(puuid)
                    .championName(engName)
                    .korName(korName)
                    .kills(sumKills / total)
                    .deaths(sumDeaths / total)
                    .assists(sumAssists / total)
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

    @Override
    public MatchOverallSummaryDTO getOverallSummary(String puuid) {
        // 전체 전적 조회
        List<MatchHistoryDTO> matchList = getMatchSummaryFromDB(puuid);

        if (matchList == null || matchList.isEmpty()) {
            return MatchOverallSummaryDTO.builder()
                    .puuid(puuid)
                    .totalMatches(0)
                    .totalWins(0)
                    .winRate(0.0)
                    .favoritePositions(Collections.emptyMap())
                    .positionCounts(Collections.emptyMap())
                    .sortedPositionList(Collections.emptyList())
                    .preferredChampions(Collections.emptyList())
                    .favoriteChampionImageUrls(Collections.emptyList())
                    .preferredPositionImageUrl(null)
                    .build();
        }

        // 전체 요약 계산
        MatchOverallSummaryDTO dto = calculateOverallSummary(matchList, puuid);

        // 이미지 URL
        dto.setFavoriteChampionImageUrls(
                dto.getPreferredChampions().stream()
                        .map(name -> imageService.getImageUrl(name + ".png", "champion"))
                        .toList()
        );
        dto.setPreferredPositionImageUrl(
                imageService.getImageUrl(dto.getPreferredPosition() + ".svg", "position")
        );

        return dto;
    }

    // 요약 + 상세 정보 -> DB 저장
    // MatchDbServiceImpl

    @Override
    public void saveMatchHistory(String puuid) {
        // 최근 matchId 20개 받아오기 (riotApiService에서)
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

        for (String matchId : matchIds) {
            // 이미 저장된 전적이면 스킵
            if (matchSummaryRepository.existsByMatchId(matchId)) continue;

            try {
                // Riot API로부터 상세 전적 받아오기
                MatchDetailDTO detail = riotApiService.getMatchDetailFromRiot(matchId, puuid);

                // null 또는 에러 시 skip
                if (detail == null) continue;

                String tier ="UNRANKED";
                // DTO -> Entity 변환
                MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid, tier);
                List<MatchPlayerEntity> players = detail.toPlayerEntities();

                System.out.println("summary = " + summary);

                // 저장
                saveMatchHistory(summary, players);

                Optional<RiotIdCacheEntity> optionalRiotId = riotIdCacheRepository.findByPuuid(puuid);

                if (optionalRiotId.isPresent()) {
                    RiotIdCacheEntity riotId = optionalRiotId.get();
                    String gameName = riotId.getGameName();
                    String tagLine = riotId.getTagLine();
                    updateOverallSummary(puuid, gameName, tagLine, tier);
                } else {
                    System.err.println("RiotIdCacheEntity not found for puuid: " + puuid);
                }

            } catch (Exception e) {
                System.err.println("[saveMatchHistory] 저장 실패 - matchId: " + matchId + " → " + e.getMessage());
            }
        }
    }

    @Override
    public boolean existsMatchByPuuid(String puuid) {
        return matchSummaryRepository.existsByPuuid(puuid);
    }

    @Override
    public void saveMatchHistory(MatchSummaryEntity summary, List<MatchPlayerEntity> players) {
        String matchId = summary.getMatchId();

        if (matchSummaryRepository.existsByMatchId(matchId)) {
            System.out.println("[saveMatchHistory] 이미 저장된 matchId: " + matchId);
            return;
        }

        matchSummaryRepository.save(summary);
        matchPlayerRepository.saveAll(players);
    }


    // 매치 전체 요약페이지 저장
    @Override
    public void saveOverallSummary(MatchOverallSummaryDTO dto) {
        MatchOverallSummaryEntity entity = MatchOverallSummaryEntity.fromDTO(dto);
        matchOverallSummaryRepository
                .findByPuuid(dto.getPuuid())
                .ifPresent(e -> entity.setId(e.getId()));

        matchOverallSummaryRepository.save(entity);
    }


    // 최신순으로 사용자의 요약 전적 20개 불러오기
    public List<MatchHistoryDTO> getMatchSummaryFromDB(String puuid) {
        List<MatchSummaryEntity> entities = matchSummaryRepository.findTop20ByPuuidOrderByGameEndTimestampDesc(puuid);

        return entities.stream()
                .map(summary -> {
                    // matchId에 해당하는 전체 player 리스트 조회
                    List<MatchPlayerEntity> playerEntities = matchPlayerRepository.findByMatchId(summary.getMatchId());
                    List<MatchPlayerDTO> playerDTOs = playerEntities.stream()
                            .map(MatchPlayerDTO::fromEntity)
                            .collect(Collectors.toList());

                    return MatchHistoryDTO.fromEntities(summary, playerDTOs, imageService);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    // matchId 기준으로 모든 플레이어 정보 가져오기
    public MatchDetailDTO getMatchDetailFromDB(String matchId) {
        List<MatchPlayerEntity> players = matchPlayerRepository.findByMatchId(matchId);

        List<MatchPlayerDTO> blueTeam = new ArrayList<>();
        List<MatchPlayerDTO> redTeam = new ArrayList<>();

        int maxDamage = players.stream()
                .mapToInt(MatchPlayerEntity::getTotalDamageDealtToChampions)
                .max().orElse(1);

        int blueGold = 0;
        int redGold = 0;
        boolean blueWin = false;

        for (MatchPlayerEntity p : players) {
            MatchPlayerDTO dto = MatchPlayerDTO.builder()
                    .matchId(p.getMatchId())
                    .summonerName(p.getSummonerName())
                    .championName(p.getChampionName())
                    .kills(p.getKills())
                    .deaths(p.getDeaths())
                    .assists(p.getAssists())
                    .kdaRatio(p.getKdaRatio())
                    .cs(p.getCs())
                    .csPerMin(p.getCsPerMin())
                    .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                    .totalDamageTaken(p.getTotalDamageTaken())
                    .teamPosition(p.getTeamPosition())
                    .mainRune1(p.getMainRune1())
                    .mainRune2(p.getMainRune2())
                    .statRune1(p.getStatRune1())
                    .statRune2(p.getStatRune2())
                    .itemIds(Arrays.asList(Optional.ofNullable(p.getItemIds()).orElse("").split(",")))
                    .wardsPlaced(p.getWardsPlaced())
                    .wardsKilled(p.getWardsKilled())
                    .gameEndTimestamp(p.getGameEndTimestamp())
                    .gameDurationMinutes(p.getGameDurationMinutes())
                    .gameDurationSeconds(p.getGameDurationSeconds())
                    .gameMode(Optional.ofNullable(p.getGameMode()).orElse("UNKNOWN"))
                    .queueType(p.getQueueType())
                    .build();

            if (p.getTeamId() == 100) {
                blueTeam.add(dto);
                blueGold += p.getGoldEarned();
                blueWin = p.isWin();
            } else {
                redTeam.add(dto);
                redGold += p.getGoldEarned();
            }
        }

        MatchObjectiveDTO blueObjectives = MatchObjectiveDTO.builder()
                .totalGold(blueGold)
                .build();

        MatchObjectiveDTO redObjectives = MatchObjectiveDTO.builder()
                .totalGold(redGold)
                .build();

        return MatchDetailDTO.builder()
                .matchId(matchId)
                .totalMaxDamage(maxDamage)
                .blueTeam(blueTeam)
                .redTeam(redTeam)
                .blueObjectives(blueObjectives)
                .redObjectives(redObjectives)
                .blueWin(blueWin)
                .build();
    }

    @Override
    public MatchDetailDTO getMatchDetailFromRiot(String matchId, String puuid) {
        return riotApiService.getMatchDetailFromRiot(matchId, puuid);
    }

    @Override
    public MatchDetailDTO getMatchDetailFromDb(String matchId, String puuid) {
        // player 리스트 조회
        List<MatchPlayerEntity> players = matchPlayerRepository.findByMatchId(matchId);
        if (players.isEmpty()) throw new RuntimeException("플레이어 데이터 없음");

        // game 정보 요약은 첫 플레이어에서 추출
        MatchPlayerEntity sample = players.get(0);

        // blue / red 팀 분류
        List<MatchPlayerDTO> blueTeam = new ArrayList<>();
        List<MatchPlayerDTO> redTeam = new ArrayList<>();
        int maxDamage = 0;
        List<String> otherSummonerNames = new ArrayList<>();

        for (MatchPlayerEntity p : players) {
            MatchPlayerDTO dto = MatchPlayerDTO.fromEntity(p);

            if (!p.getPuuid().equals(puuid)) {
                otherSummonerNames.add(p.getSummonerName());
            }

            // 데미지 최대값 계산
            if (p.getDamageDealt() != null) {
                maxDamage = Math.max(maxDamage, p.getDamageDealt());
            }

            if (p.getTeamId() == 100) {
                blueTeam.add(dto);
            } else {
                redTeam.add(dto);
            }
        }

        // 블루팀 승리 여부 판단 (teamId 100 중 한 명 win)
        boolean blueWin = players.stream()
                .anyMatch(p -> p.getTeamId() == 100 && p.isWin());

        int redTotalKills = redTeam.stream().mapToInt(MatchPlayerDTO::getKills).sum();
        int blueTotalKills = blueTeam.stream().mapToInt(MatchPlayerDTO::getKills).sum();

        int redTotalGold = redTeam.stream().mapToInt(MatchPlayerDTO::getGoldEarned).sum();
        int blueTotalGold = blueTeam.stream().mapToInt(MatchPlayerDTO::getGoldEarned).sum();

        MatchObjectiveDTO redObjectives = MatchObjectiveDTO.builder()
                .totalKills(redTotalKills)
                .totalGold(redTotalGold)
                .build();

        MatchObjectiveDTO blueObjectives = MatchObjectiveDTO.builder()
                .totalKills(blueTotalKills)
                .totalGold(blueTotalGold)
                .build();

        return MatchDetailDTO.builder()
                .matchId(matchId)
                .myPuuid(puuid)
                .gameEndTimestamp(sample.getGameEndTimestamp())
                .gameDurationSeconds(sample.getGameDurationSeconds())
                .gameMode(sample.getGameMode())
                .queueType(sample.getQueueType())
                .blueTeam(blueTeam)
                .redTeam(redTeam)
                .blueWin(blueWin)
                .blueObjectives(blueObjectives)
                .redObjectives(redObjectives)
                .totalMaxDamage(maxDamage)
                .otherSummonerNames(otherSummonerNames)
                .build();
    }

    @Override
    public MatchSummaryWithListDTO getSummaryAndList(String puuid) {
        List<MatchHistoryDTO> matchList = getMatchSummaryFromDB(puuid);
        MatchSummaryDTO summary = calculateSummary(matchList);
        List<FavoriteChampionDTO> favoriteChampions = getFavoriteChampions(puuid, "overall");

        return MatchSummaryWithListDTO.builder()
                .matchList(matchList)
                .summary(summary)
                .favoriteChampions(favoriteChampions)
                .build();
    }

    private MatchSummaryDTO calculateSummary(List<MatchHistoryDTO> matchList) {
        MatchSummaryDTO dto = new MatchSummaryDTO();

        int total = matchList.size();
        int wins = 0;

        double totalKills = 0.0;
        double totalDeaths = 0.0;
        double totalAssists = 0.0;
        double totalKillParticipation = 0.0;

        Map<String, Integer> championWins = new HashMap<>();
        Map<String, Integer> championTotalGames = new HashMap<>();

        for (MatchHistoryDTO match : matchList) {
            if (match.isWin()) wins++;

            totalKills += match.getKills();
            totalDeaths += match.getDeaths();
            totalAssists += match.getAssists();
            totalKillParticipation += match.getKillParticipation();

            String championName = match.getChampionName();
            championTotalGames.put(championName, championTotalGames.getOrDefault(championName, 0) + 1);
            if (match.isWin()) {
                championWins.put(championName, championWins.getOrDefault(championName, 0) + 1);
            }
        }

        dto.setTotalCount(total);
        dto.setWinCount(wins);
        dto.setLoseCount(total - wins);

        if (total > 0) {
            dto.setAverageKills(totalKills / total);
            dto.setAverageDeaths(totalDeaths / total);
            dto.setAverageAssists(totalAssists / total);

            double kda = (totalDeaths > 0) ? (totalKills + totalAssists) / totalDeaths : (totalKills + totalAssists);
            dto.setAverageKda(kda);
            dto.setKillParticipation(totalKillParticipation / total);
        }

        dto.setChampionWins(championWins);
        dto.setChampionTotalGames(championTotalGames);

        // 승률 계산
        Map<String, Double> championWinRates = new HashMap<>();
        for (String champ : championTotalGames.keySet()) {
            int win = championWins.getOrDefault(champ, 0);
            int count = championTotalGames.get(champ);
            double winRate = (count > 0) ? (win * 100.0 / count) : 0.0;
            championWinRates.put(champ, winRate);
        }
        dto.setChampionWinRates(championWinRates);

        // 출현 빈도순 정렬
        List<Map.Entry<String, Integer>> sortedChampionList = championTotalGames.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        dto.setSortedChampionList(sortedChampionList);

        return dto;
    }


    @Override
    public void updateMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);

        for (String matchId : matchIds) {
            if (matchSummaryRepository.existsByMatchId(matchId)) continue;

            // matchId 기반으로 Riot API로부터 상세 전적 받아오기
            MatchDetailDTO detail = getMatchDetailFromRiot(matchId, puuid);

            String tier ="UNRANKED";
            // 요약 정보로 변환하여 저장
            MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid, tier);
            matchSummaryRepository.save(summary);

            // 참가자 정보 저장 // 내부 DTO( MatchPlayerDTO ) -> Entity 로 저장
            for (MatchPlayerDTO player : detail.getBlueTeam()) {
                matchPlayerRepository.save(MatchPlayerEntity.fromDTO(player));
            }
            for (MatchPlayerDTO player : detail.getRedTeam()) {
                matchPlayerRepository.save(MatchPlayerEntity.fromDTO(player));
            }
        }
    }

    @Override
    public List<MatchHistoryDTO> getRecentMatchHistories(String puuid) {
        return matchSummaryRepository.findTop20ByPuuidOrderByGameEndTimestampDesc(puuid)
                .stream()
                .map(MatchSummaryEntity::toDTO)
                .collect(Collectors.toList());
    }

    // DB 에서 가져온 entity -> dto
    @Override
    public List<FavoriteChampionDTO> getFavoriteChampions(String puuid, String queueType) {
        return favoriteChampionRepository.findByPuuidAndQueueType(puuid, queueType).stream()
                .map(entity -> {
                    FavoriteChampionDTO dto = FavoriteChampionDTO.fromEntity(entity);

                    // 챔피언 이미지 URL
                    dto.setChampionImageUrl(
                            imageService.getImageUrl(entity.getChampionName() + ".png", "champion")
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }


//    public void saveOnlyOverallSummary(String gameName, String tagLine, String tier) {
//        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);
//
//        Optional<RiotIdCacheEntity> riotIdOpt = riotIdCacheRepository.findByPuuid(puuid);
//        if (riotIdOpt.isPresent()) {
//            RiotIdCacheEntity riotId = riotIdOpt.get();
//            updateOverallSummary(puuid, riotId.getGameName(), riotId.getTagLine());
//        } else {
//            log.warn("RiotIdCacheEntity not found for puuid: {}", puuid);
//        }
//    }

    // 각 매치 요약과 플레이어 상세정보
    public void saveMatchSummaryAndPlayers(String gameName, String tagLine, String tier) {
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

        for (String matchId : matchIds) {
            try {
                MatchDetailDTO matchDetail = riotApiService.getMatchDetailFromRiot(matchId, puuid);
                MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(matchDetail, puuid, tier);
                if (summary == null) continue;
                matchSummaryRepository.save(summary);

                int durationSec = matchDetail.getGameDurationSeconds();
                LocalDateTime endTime = matchDetail.getGameEndTimestamp();
                String gameMode = matchDetail.getGameMode();
                String queueType = matchDetail.getQueueType();

                List<MatchPlayerDTO> playerList = MatchPlayerDTO.fromRiotParticipantList(
                        matchDetail.getParticipants(), matchId, durationSec, endTime, gameMode, queueType, tier);

                for (MatchPlayerDTO player : playerList) {
                    MatchPlayerEntity entity = MatchPlayerEntity.fromDTO(player);
                    matchPlayerRepository.save(entity);
                }
            } catch (Exception e) {
                log.error("매치 저장 오류 : " + matchId, e);
            }
        }
    }

    // 각 매치 요약을 이용하여 매치 전체 요약 정보
    public void saveOnlyOverallSummary(String gameName, String tagLine, String tier) {
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);

        updateOverallSummary(puuid, gameName, tagLine, tier);
    }


    // MatchHistoryDTO
    public List<MatchHistoryDTO> fetchFavoriteChampionMatches(String gameName, String tagLine) {
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 30);
        List<MatchHistoryDTO> matchList = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);
                RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
                if (participant == null) continue;

                int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
                double csPerMin = cs / (matchInfo.getGameDurationSeconds() / 60.0);
                int teamId = participant.getTeamId();
                int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

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
                        .gameEndTimestamp(
                                Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime())
                        .killParticipation(MatchHelper.calculateKillParticipation(
                                participant.getKills(), participant.getAssists(), teamTotalKills))
                        .build();

                matchList.add(dto);
            } catch (Exception e) {
                log.warn("[favorite] 매치 분석 실패: " + matchId);
            }
        }

        return matchList;
    }




    @Override
    public void testSave(String gameName, String tagLine, String tier) {
        // Riot API로 puuid 가져오기
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);

        // 최근 matchId 리스트 가져오기
        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 20);

        System.out.println("matchIds 개수: " + matchIds.size());

        for (String matchId : matchIds) {
            try {
                // 상세 전적 정보 가져오기
                MatchDetailDTO matchDetail = riotApiService.getMatchDetailFromRiot(matchId, puuid);

                // match_summary 저장
                MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(matchDetail, puuid, tier);
                if (summary == null) {
                    System.err.println("summary == null, puuid 못 찾음 - matchId: " + matchId);
                    continue;
                }

                System.out.println("matchDetail.getParticipants().size() = " + matchDetail.getParticipants().size());

                matchSummaryRepository.save(summary);

                // match_player 저장

                int durationSec = matchDetail.getGameDurationSeconds();
                LocalDateTime endTime = matchDetail.getGameEndTimestamp();
                String gameMode = matchDetail.getGameMode();
                String queueType = matchDetail.getQueueType();

                List<MatchPlayerDTO> playerList = MatchPlayerDTO.fromRiotParticipantList(matchDetail.getParticipants(), matchId,durationSec,
                        endTime, gameMode, queueType, tier);

                for (MatchPlayerDTO player : playerList) {
                    MatchPlayerEntity entity = MatchPlayerEntity.fromDTO(player);
                    matchPlayerRepository.save(entity);
                }
            } catch (Exception e) {
                log.error("매치 저장 오류 : " + matchId, e);
            }
        }
        System.out.println("match-player 저장");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<MatchSummaryEntity> summaryList = matchSummaryRepository.findByPuuid(puuid);

        List<MatchHistoryDTO> matchList = summaryList.stream()
                .map(summary -> {
                    List<MatchPlayerEntity> players = matchPlayerRepository.findByMatchId(summary.getMatchId());
                    List<MatchPlayerDTO> playerDTOs = players.stream()
                            .map(MatchPlayerDTO::fromEntity)
                            .toList();
                    return MatchHistoryDTO.fromEntities(summary, playerDTOs, imageService);
                })
                .toList();

        List<String> matchIdsForFavorite = riotApiService.getRecentMatchIds(puuid, 30);
        List<MatchHistoryDTO> matchListForFavorite = new ArrayList<>();

        for (String matchId : matchIdsForFavorite) {
            try {
                RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);
                RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
                if (participant == null) continue;

                int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
                double csPerMin = cs / (matchInfo.getGameDurationSeconds() / 60.0);
                int teamId = participant.getTeamId();
                int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

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
                        .gameEndTimestamp(
                                Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime())
                        .killParticipation(MatchHelper.calculateKillParticipation(
                                participant.getKills(), participant.getAssists(), teamTotalKills
                        ))
                        .build();

                matchListForFavorite.add(dto);


            } catch (Exception e) {
                log.warn("[favorite] 매치 분석 실패: " + matchId);
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("calculateFavoriteChampions 호출 전");
        // favorite 챔피언 계산 및 저장
        List<FavoriteChampionDTO> dtoList = calculateFavoriteChampions(matchListForFavorite, "all", puuid);
        System.out.println("FavoriteChampionDTO 결과 dtoList = " + dtoList);
        saveFavoriteChampions(puuid, dtoList);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // overall summary 계산 및 저장
        Optional<RiotIdCacheEntity> riotIdOpt = riotIdCacheRepository.findByPuuid(puuid);
        if (riotIdOpt.isPresent()) {
            RiotIdCacheEntity riotId = riotIdOpt.get();
            updateOverallSummary(puuid, riotId.getGameName(), riotId.getTagLine(), tier);
        } else {
            log.warn("RiotIdCacheEntity not found for puuid: {}", puuid);
        }
//        Optional<RiotIdCacheEntity> riotIdOpt = riotIdCacheRepository.findByPuuid(puuid);
//        if (riotIdOpt.isPresent()) {
//            RiotIdCacheEntity riotId = riotIdOpt.get();
//            List<MatchSummaryEntity> summaries = matchSummaryRepository.findByPuuid(puuid);
//
//            int wins = 0;
//            double kills = 0, deaths = 0, assists = 0;
//            Map<String, Integer> positionMap = new HashMap<>();
//
//            for (MatchSummaryEntity summary : summaries) {
//                if (summary.isWin()) wins++;
//
//                kills += summary.getKills();
//                deaths += summary.getDeaths();
//                assists += summary.getAssists();
//
//                String pos = summary.getTeamPosition();
//                if (pos != null) {
//                    positionMap.put(pos, positionMap.getOrDefault(pos, 0) + 1);
//                }
//            }
//
//            double winRate = summaries.isEmpty() ? 0.0 : wins * 100.0 / summaries.size();
//            double kda = deaths == 0 ? kills + assists : (kills + assists) / deaths;
//
//            String preferredPosition = positionMap.entrySet().stream()
//                    .max(Map.Entry.comparingByValue())
//                    .map(Map.Entry::getKey)
//                    .orElse("UNKNOWN");
//
//            MatchOverallSummaryEntity entity = MatchOverallSummaryEntity.builder()
//                    .puuid(puuid)
//                    .gameName(riotId.getGameName())
//                    .tagLine(riotId.getTagLine())
//                    .tier(tier)
//                    .totalMatches(summaries.size())
//                    .totalWins(wins)
//                    .winRate(winRate)
//                    .averageKda(kda)
//                    .preferredPosition(preferredPosition)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//
//            matchOverallSummaryRepository.save(entity);
//
//        } else {
//            log.warn("RiotIdCacheEntity not found for puuid: {}", puuid);
//        }
    }

    private void fillMissingTier(List<MatchPlayerDTO> playerList) {
        Set<String> visited = new HashSet<>();

        for (MatchPlayerDTO player : playerList) {
            String puuid = player.getPuuid();

            if (visited.contains(puuid)) continue;
            visited.add(puuid);
//
//            if (player.getTier() == null || player.getTier().isBlank()) {
//                try {
//                    String tier = riotApiService.getTierByPuuid(puuid);
//                    player.setTier(tier);
//                    Thread.sleep(150); // 키...
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    log.warn("티어 조회 실패: {}", puuid, e);
//                }
//            }
        }

    }
//
//    @Override
//    public void saveOnlyOverallSummary(String gameName, String tagLine, String tier) {
//        // puuid 조회
//        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);
//
//        updateOverallSummary(puuid, gameName, tagLine);
//
//        // RiotIdCacheEntity 조회
//        Optional<MatchOverallSummaryEntity> optional = matchOverallSummaryRepository.findByPuuid(puuid);
//        if (optional.isPresent()) {
//            MatchOverallSummaryEntity entity = optional.get();
//            entity.setTier(tier);
//            matchOverallSummaryRepository.save(entity);
//            log.info("match_overall_summary 저장 후 티어 수동 덮어쓰기 완료: {}#{} [{}]", gameName, tagLine, tier);
//        } else {
//            log.warn("match_overall_summary not found for puuid: {}", puuid);
//        }
//    }


    @Override
    public MatchOverallSummaryDTO calculateOverallSummary(List<MatchHistoryDTO> matchList, String puuid) {
        if (matchList == null || matchList.isEmpty()) return null;

        int totalMatches = matchList.size();
        int totalWins = (int) matchList.stream().filter(MatchHistoryDTO::isWin).count();
        double winRate = round((double) totalWins / totalMatches * 100.0, 0);
        double averageKills = matchList.stream().mapToInt(MatchHistoryDTO::getKills).average().orElse(0);
        double averageDeaths = matchList.stream().mapToInt(MatchHistoryDTO::getDeaths).average().orElse(0);
        double averageAssists = matchList.stream().mapToInt(MatchHistoryDTO::getAssists).average().orElse(0);
        double averageKDA = matchList.stream().mapToDouble(MatchHistoryDTO::getKdaRatio).average().orElse(0);
        double averageCs = matchList.stream().mapToInt(MatchHistoryDTO::getCs).average().orElse(0);

        Map<String, Long> positionCounts = matchList.stream()
                .map(match -> MatchHelper.normalizePosition(match.getTeamPosition()))
                .filter(pos -> pos != null && !pos.equals("unknown"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        int totalPositions = positionCounts.values().stream().mapToInt(Long::intValue).sum();

        Map<String, Double> favoritePositions = positionCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> round((double) e.getValue() * 100.0 / totalPositions, 1)
                ));
        // 모든 포지션 기본값 세팅
        List<String> allPositions = List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

        for (String pos : allPositions) {
            favoritePositions.putIfAbsent(pos, 0.0);
        }

        List<String> sortedPositionList = allPositions.stream()
                .sorted(Comparator.comparingDouble((String pos) ->
                        -favoritePositions.getOrDefault(pos, 0.0)))
                .toList();

        // 포지션 선호도
        String preferredPosition = positionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        log.info("favoritePositions = {}", favoritePositions);

        // 챔피언별 경기 수
        Map<String, Integer> championTotalGames = matchList.stream()
                .collect(Collectors.toMap(
                        MatchHistoryDTO::getChampionName,
                        m -> 1,
                        Integer::sum // 중복 키 처리
                ));

        // 챔피언 선호도 (이름: 사용횟수 JSON)
        Map<String, Long> championUsage = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName, Collectors.counting()));

        List<String> top3Champions = championUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 챔피언별 승리 횟수
        Map<String, Integer> championWins = matchList.stream()
                .filter(MatchHistoryDTO::isWin)
                .collect(Collectors.groupingBy(
                        MatchHistoryDTO::getChampionName,
                        Collectors.reducing(0, e -> 1, Integer::sum)
                ));

        // 챔피언별 승률
        Map<String, List<MatchHistoryDTO>> groupedByChampion = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName));

        Map<String, Double> championWinRates = groupedByChampion.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<MatchHistoryDTO> games = e.getValue();
                            long wins = games.stream().filter(MatchHistoryDTO::isWin).count();
                            return round((double) wins / games.size() * 100.0, 1);
                        }
                ));

        // 게임네임/태그 가져오기 (캐시에서)
        RiotIdCacheEntity riotId = riotIdCacheRepository.findByPuuid(puuid)
                .orElse(RiotIdCacheEntity.ofDummy(puuid));

        List<Map.Entry<String, Long>> sortedChampionList = championUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        return MatchOverallSummaryDTO.builder()
                .puuid(puuid)
                .gameName(riotId.getGameName())
                .tagLine(riotId.getTagLine())
                .totalMatches(totalMatches)
                .totalWins(totalWins)
                .winRate(round(winRate,0))
                .averageKills(round(averageKills, 1))
                .averageDeaths(round(averageDeaths, 1))
                .averageAssists(round(averageAssists, 1))
                .averageKda(round(averageKDA, 2))
                .averageCs(round(averageCs, 1))
                .preferredPosition(preferredPosition)
                .preferredChampions(top3Champions)
                .positionCounts(positionCounts)
                .favoritePositions(favoritePositions)
                .sortedPositionList(sortedPositionList)
                .sortedChampionList(sortedChampionList)
                .championTotalGames(championTotalGames)
                .championWins(championWins)
                .championWinRates(championWinRates)
                .build();
    }

    // favorite_champion 테이블 -> dto로 map 리턴
    @Override
    public Map<String, List<FavoriteChampionDTO>> getFavoriteChampionsAll(String puuid) {
        Map<String, List<FavoriteChampionDTO>> result = new HashMap<>();

        // 큐 타입별 조회
        result.put("overall", getFavoriteChampions(puuid, "overall"));
        result.put("solo", getFavoriteChampions(puuid, "solo"));
        result.put("flex", getFavoriteChampions(puuid, "flex"));

        return result;
    }

    // 선호챔피언 저장
    @Override
    @Transactional
    public void saveFavoriteChampions(String puuid, List<FavoriteChampionDTO> dtoList) {

        if (dtoList.isEmpty()) return;

        String mode = dtoList.get(0).getQueueType();

        favoriteChampionRepository.deleteByPuuidAndQueueType(puuid, mode);

        List<FavoriteChampionEntity> entities = dtoList.stream()
                .map(FavoriteChampionEntity::fromDTO)
                .collect(Collectors.toList());
        favoriteChampionRepository.saveAll(entities);
    }

    // dto -> entity
    @Override
    @Transactional
    public void saveFavoriteChampionOnly(String gameName, String tagLine) {
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);

        List<String> matchIds = riotApiService.getRecentMatchIds(puuid, 40);
        List<MatchHistoryDTO> matchListForFavorite = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                RiotMatchInfoDTO matchInfo = riotApiService.getMatchInfo(matchId);
                RiotParticipantDTO participant = matchInfo.getParticipantByPuuid(puuid);
                if (participant == null) continue;

                int cs = participant.getTotalMinionsKilled() + participant.getNeutralMinionsKilled();
                double csPerMin = cs / (matchInfo.getGameDurationSeconds() / 60.0);
                int teamId = participant.getTeamId();
                int teamTotalKills = MatchHelper.getTeamTotalKills(matchInfo.getParticipants(), teamId);

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
                        .gameEndTimestamp(
                                Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime())
                        .killParticipation(MatchHelper.calculateKillParticipation(
                                participant.getKills(), participant.getAssists(), teamTotalKills))
                        .build();

                matchListForFavorite.add(dto);

            } catch (Exception e) {
                log.warn("[favorite] 매치 분석 실패: " + matchId);
            }
        }

        for (String mode : List.of( "overall")) {
            List<FavoriteChampionDTO> dtoList = calculateFavoriteChampions(matchListForFavorite, mode, puuid);
            saveFavoriteChampions(puuid, dtoList);
        }
    }





    @Override
    public void overwriteTier(String gameName, String tagLine, String tier) {
        String puuid = riotApiService.requestPuuidFromRiot(gameName, tagLine);

        Optional<MatchOverallSummaryEntity> opt = matchOverallSummaryRepository.findByPuuid(puuid);
        if (opt.isPresent()) {
            MatchOverallSummaryEntity entity = opt.get();
            entity.setTier(tier);
            matchOverallSummaryRepository.save(entity);
            log.info("티어 덮어쓰기 완료: {}#{} → {}", gameName, tagLine, tier);
        } else {
            log.warn("match_overall_summary 없음: {}#{}", gameName, tagLine);
        }
    }



}
