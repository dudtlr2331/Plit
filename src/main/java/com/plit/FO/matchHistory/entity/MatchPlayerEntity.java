package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_player")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerEntity { // 매치 상세페이지 데이터 저장

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String puuid;
    private String matchId;
    private String summonerName;
    private String championName;

    private int kills;
    private int deaths;
    private int assists;
    private double kdaRatio;
    private int damageDealt;
    private int damageTaken;

    private int cs; // 미니언 처치수
    private double csPerMin;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private String teamPosition;
    private String tier;
    private int champLevel;

    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;

    private int wardsPlaced;
    private int wardsKilled;

    private LocalDateTime gameEndTimestamp;
    private int gameDurationMinutes;
    private int gameDurationSeconds;

    private String gameMode;
    private String queueType;

    private int teamId;
    private boolean win;

    private int goldEarned;

    // 문자열로 콤마(,) 구분하여 저장
    private String itemIds;

    // dto -> entity ( db 저장 전에 가공 )
    public static MatchPlayerEntity fromDTO(MatchPlayerDTO dto) {
        return MatchPlayerEntity.builder()
                .summonerName(dto.getSummonerName())
                .championName(dto.getChampionName())
                .kills(dto.getKills())
                .deaths(dto.getDeaths())
                .assists(dto.getAssists())
                .kdaRatio(dto.getKdaRatio())
                .damageDealt(dto.getTotalDamageDealtToChampions())
                .damageTaken(dto.getTotalDamageTaken())
                .cs(dto.getCs())
                .csPerMin(dto.getCsPerMin())
                .totalDamageDealtToChampions(dto.getTotalDamageDealtToChampions())
                .totalDamageTaken(dto.getTotalDamageTaken())
                .teamPosition(dto.getTeamPosition())
                .tier(dto.getTier())
                .champLevel(dto.getChampionLevel())
                .mainRune1(dto.getMainRune1())
                .mainRune2(dto.getMainRune2())
                .statRune1(dto.getStatRune1())
                .statRune2(dto.getStatRune2())
                .wardsPlaced(dto.getWardsPlaced())
                .wardsKilled(dto.getWardsKilled())
                .gameEndTimestamp(dto.getGameEndTimestamp())
                .gameDurationMinutes(dto.getGameDurationMinutes())
                .gameDurationSeconds(dto.getGameDurationSeconds())
                .gameMode(dto.getGameMode())
                .queueType(dto.getQueueType())
                .teamId(dto.getTeamId())
                .win(dto.isWin())
                .goldEarned(dto.getGoldEarned())
                .itemIds(String.join(",", dto.getItemIds()))
                .build();
    }

}
