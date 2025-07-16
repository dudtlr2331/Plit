package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.dto.MatchObjectiveDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailDTO { // 최근 매치 상세 정보 전체 유저

    private List<RiotParticipantDTO> participants;

    private String gameMode;
    private LocalDateTime gameEndTimestamp;

    private String matchId;
    private int totalMaxDamage;

    private List<MatchPlayerDTO> blueTeam;
    private List<MatchPlayerDTO> redTeam;

    private MatchObjectiveDTO blueObjectives;
    private MatchObjectiveDTO redObjectives;
    private boolean blueWin;

    private int calculateMaxDamage() {
        if (participants == null || participants.isEmpty()) return 0;

        return participants.stream()
                .mapToInt(RiotParticipantDTO::getTotalDamageDealtToChampions)
                .max()
                .orElse(0);
    }

    public List<MatchPlayerEntity> toPlayerEntities() {
        if (participants == null || participants.isEmpty()) return new ArrayList<>();

        return participants.stream()
                .map(p -> MatchPlayerEntity.builder()
                        .matchId(matchId)
                        .puuid(p.getPuuid())
                        .summonerName(p.getSummonerName())
                        .championName(p.getChampionName())
                        .teamPosition(p.getTeamPosition())
                        .kills(p.getKills())
                        .deaths(p.getDeaths())
                        .assists(p.getAssists())
                        .win(p.isWin())
                        .tier(p.getTier())
                        .champLevel(p.getChampLevel())
                        .cs(p.getTotalMinionsKilled())
                        .itemIds(convertItemIds(p.getItemIds()))
                        .damageDealt(p.getTotalDamageDealtToChampions())
                        .damageTaken(p.getTotalDamageTaken())
                        .gameMode(gameMode)
                        .gameEndTimestamp(gameEndTimestamp)
                        .build())
                .collect(Collectors.toList());
    }

    private String convertItemIds(List<Integer> itemIds) {
        if (itemIds == null) return "";
        return itemIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public MatchDetailDTO(RiotMatchInfoDTO matchInfo, String matchId) {
        this.matchId = matchId;
        this.gameMode = matchInfo.getGameMode();
        this.gameEndTimestamp = Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        this.participants = matchInfo.getParticipants();

        // 초기값 세팅
        this.totalMaxDamage = calculateMaxDamage();
        this.blueTeam = new ArrayList<>();
        this.redTeam = new ArrayList<>();
        this.blueObjectives = new MatchObjectiveDTO();
        this.redObjectives = new MatchObjectiveDTO();

        // 승리 여부: 팀 ID 100이 승리한 경우 기준
        this.blueWin = participants.stream()
                .filter(p -> p.getTeamId() == 100)
                .findFirst()
                .map(RiotParticipantDTO::isWin)
                .orElse(true);  // 기본값 true
    }


}