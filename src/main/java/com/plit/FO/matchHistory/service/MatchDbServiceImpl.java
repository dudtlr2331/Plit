package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.MatchPlayerRepository;
import com.plit.FO.matchHistory.repository.MatchSummaryRepository;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class MatchDbServiceImpl implements MatchDbService{ // 전적 검색 DB 저장, 조회

    private final MatchSummaryRepository matchSummaryRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final RiotApiService riotApiService;
    private final RiotIdCacheRepository riotIdCacheRepository;

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
                    .kdaRatio(calculateKda(me.getKills(), me.getDeaths(), me.getAssists()))
                    .tier(riotApiService.getTierByPuuid(puuid))
                    .gameEndTimestamp(endTime)
                    .gameMode(info.getGameMode())
                    .champLevel(me.getChampLevel())
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
                            .kdaRatio(calculateKda(p.getKills(), p.getDeaths(), p.getAssists()))
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

                // 저장
                saveMatchHistory(summary, players);

            } catch (Exception e) {
                System.err.println("[saveMatchHistory] 저장 실패 - matchId: " + matchId + " → " + e.getMessage());
            }
        }
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
                        .gameMode(entity.getGameMode())
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
                    .gameMode(p.getGameMode())
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




}
