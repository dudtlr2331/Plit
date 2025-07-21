package com.plit.FO.matchHistory.service;

import com.nimbusds.jose.shaded.gson.Gson;
import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.*;
import com.plit.FO.matchHistory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
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
                    .championLevel(me.getChampionLevel())
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
                            .tier(riotApiService.getTierByPuuid(p.getPuuid()))
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

    public void updateOverallSummary(String puuid, String gameName, String tagLine) {
        // 해당 유저의 모든 전적 요약 가져오기
        List<MatchSummaryEntity> matchList = matchSummaryRepository.findByPuuid(puuid);

        // 요약 통계 계산
        MatchOverallSummaryDTO dto = MatchHelper.getOverallSummary(puuid, gameName, tagLine, matchList);

        Optional<MatchOverallSummaryEntity> existing = matchOverallSummaryRepository.findByPuuid(puuid);

        // DTO -> Entity 변환
        MatchOverallSummaryEntity entity = MatchOverallSummaryEntity.fromDTO(dto);

        if (existing.isPresent()) {
            entity.setId(existing.get().getId());
        }

        // 저장
        matchOverallSummaryRepository.save(entity);
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
                    .queueType(mode)
                    .build();

            result.add(dto);
        }

        System.out.println("[calculateFavoriteChampions] mode: " + mode + ", result size: " + result.size());
        result.forEach(dto -> System.out.println(" - " + dto.getChampionName() + " (" + dto.getGameCount() + " games)"));

        result.sort(Comparator.comparingInt(FavoriteChampionDTO::getFlexGames).reversed());
        return result;
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

                // DTO -> Entity 변환
                MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid);
                List<MatchPlayerEntity> players = detail.toPlayerEntities();

                System.out.println("summary = " + summary);

                // 저장
                saveMatchHistory(summary, players);

                Optional<RiotIdCacheEntity> optionalRiotId = riotIdCacheRepository.findByPuuid(puuid);

                if (optionalRiotId.isPresent()) {
                    RiotIdCacheEntity riotId = optionalRiotId.get();
                    String gameName = riotId.getGameName();
                    String tagLine = riotId.getTagLine();
                    updateOverallSummary(puuid, gameName, tagLine);
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
                .map(entity -> MatchHistoryDTO.builder()
                        .matchId(entity.getMatchId())
                        .win(entity.isWin())
                        .teamPosition(entity.getTeamPosition())
                        .championName(entity.getChampionName())
                        .kills(entity.getKills())
                        .deaths(entity.getDeaths())
                        .assists(entity.getAssists())
                        .kdaRatio(entity.getKdaRatio())
                        .tier(entity.getTier())
                        .gameEndTimestamp(entity.getGameEndTimestamp())
                        .gameMode(Optional.ofNullable(entity.getGameMode()).orElse("UNKNOWN"))
                        .build())
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
                    .tier(p.getTier())
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
                .blueObjectives(new MatchObjectiveDTO())
                .redObjectives(new MatchObjectiveDTO())
                .totalMaxDamage(maxDamage)
                .otherSummonerNames(otherSummonerNames)
                .build();
    }

    @Override
    public MatchSummaryWithListDTO getSummaryAndList(String puuid) {
        List<MatchHistoryDTO> matchList = getMatchSummaryFromDB(puuid);
        MatchOverallSummaryDTO summary = calculateOverallSummary(matchList, puuid);
        List<FavoriteChampionDTO> favoriteChampions = getFavoriteChampions(puuid, "overall");

        return MatchSummaryWithListDTO.builder()
                .matchList(matchList)
                .summary(summary)
                .favoriteChampions(favoriteChampions)
                .build();
    }

    @Override
    public void updateMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);

        for (String matchId : matchIds) {
            if (matchSummaryRepository.existsByMatchId(matchId)) continue;

            // matchId 기반으로 Riot API로부터 상세 전적 받아오기
            MatchDetailDTO detail = getMatchDetailFromRiot(matchId, puuid);

            // 요약 정보로 변환하여 저장
            MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid);
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

    @Override
    public List<FavoriteChampionDTO> getFavoriteChampions(String puuid, String queueType) {
        return favoriteChampionRepository.findByPuuidAndQueueType(puuid, queueType).stream()
                .map(FavoriteChampionDTO::fromEntity)
                .collect(Collectors.toList());
    }


    @Override
    public void testSave(String gameName, String tagLine) {
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
                MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(matchDetail, puuid);
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
                        endTime, gameMode, queueType);
                fillMissingTier(playerList);

                for (MatchPlayerDTO player : playerList) {
                    MatchPlayerEntity entity = MatchPlayerEntity.fromDTO(player);
                    matchPlayerRepository.save(entity);
                }

                Thread.sleep(1200); // 키 제한..
            } catch (Exception e) {
                log.error("매치 저장 오류 : " + matchId, e);
            }
        }
        System.out.println("match 저장 끝남");

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

        List<String> matchIdsForFavorite = riotApiService.getRecentMatchIds(puuid, 60);
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
                        .csPerMin(csPerMin)
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

        System.out.println("calculateFavoriteChampions 호출 전");
        // favorite 챔피언 계산 및 저장
        List<FavoriteChampionDTO> dtoList = calculateFavoriteChampions(matchListForFavorite, "all", puuid);
        System.out.println("FavoriteChampionDTO 결과 dtoList = " + dtoList);
        saveFavoriteChampions(puuid, dtoList);

        // overall summary 계산 및 저장
        Optional<RiotIdCacheEntity> riotIdOpt = riotIdCacheRepository.findByPuuid(puuid);
        if (riotIdOpt.isPresent()) {
            RiotIdCacheEntity riotId = riotIdOpt.get();
            updateOverallSummary(puuid, riotId.getGameName(), riotId.getTagLine());
        } else {
            log.warn("RiotIdCacheEntity not found for puuid: {}", puuid);
        }


    }

    private void fillMissingTier(List<MatchPlayerDTO> playerList) {
        Set<String> visited = new HashSet<>();

        for (MatchPlayerDTO player : playerList) {
            String puuid = player.getPuuid();

            if (visited.contains(puuid)) continue;
            visited.add(puuid);

            if (player.getTier() == null || player.getTier().isBlank()) {
                try {
                    String tier = riotApiService.getTierByPuuid(puuid);
                    player.setTier(tier);
                    Thread.sleep(150); // 키...
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.warn("티어 조회 실패: {}", puuid, e);
                }
            }
        }

    }


    @Override
    public MatchOverallSummaryDTO calculateOverallSummary(List<MatchHistoryDTO> matchList, String puuid) {
        if (matchList == null || matchList.isEmpty()) return null;

        int totalMatches = matchList.size();
        int totalWins = (int) matchList.stream().filter(MatchHistoryDTO::isWin).count();
        double winRate = round((double) totalWins / totalMatches * 100.0, 1);
        double avgKills = matchList.stream().mapToInt(MatchHistoryDTO::getKills).average().orElse(0);
        double avgDeaths = matchList.stream().mapToInt(MatchHistoryDTO::getDeaths).average().orElse(0);
        double avgAssists = matchList.stream().mapToInt(MatchHistoryDTO::getAssists).average().orElse(0);
        double avgKDA = matchList.stream().mapToDouble(MatchHistoryDTO::getKdaRatio).average().orElse(0);
        double avgCs = matchList.stream().mapToInt(MatchHistoryDTO::getCs).average().orElse(0);

        Map<String, Long> positionCounts = matchList.stream()
                .filter(m -> m.getTeamPosition() != null)
                .collect(Collectors.groupingBy(MatchHistoryDTO::getTeamPosition, Collectors.counting()));

        // 포지션 선호도
        String preferredPosition = positionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        // 챔피언 선호도 (이름: 사용횟수 JSON)
        Map<String, Long> championUsage = matchList.stream()
                .collect(Collectors.groupingBy(MatchHistoryDTO::getChampionName, Collectors.counting()));

        List<String> top3Champions = championUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 게임네임/태그 가져오기 (캐시에서)
        RiotIdCacheEntity riotId = riotIdCacheRepository.findByPuuid(puuid)
                .orElse(RiotIdCacheEntity.ofDummy(puuid));

        return MatchOverallSummaryDTO.builder()
                .puuid(puuid)
                .gameName(riotId.getGameName())
                .tagLine(riotId.getTagLine())
                .totalMatches(totalMatches)
                .totalWins(totalWins)
                .winRate(winRate)
                .averageKills(round(avgKills, 1))
                .averageDeaths(round(avgDeaths, 1))
                .averageAssists(round(avgAssists, 1))
                .averageKda(round(avgKDA, 2))
                .averageCs(round(avgCs, 1))
                .preferredPosition(preferredPosition)
                .preferredChampions(top3Champions)
                .positionCounts(positionCounts)
                .build();
    }

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
    public void saveFavoriteChampions(String puuid, List<FavoriteChampionDTO> dtoList) {
        // 기존 데이터 삭제 후 저장 (upsert 방식)
        favoriteChampionRepository.deleteAll(favoriteChampionRepository.findByPuuid(puuid));

        List<FavoriteChampionEntity> entities = dtoList.stream()
                .map(FavoriteChampionEntity::fromDTO)
                .collect(Collectors.toList());
        favoriteChampionRepository.saveAll(entities);
    }


}
