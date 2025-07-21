package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.service.MatchHelper;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "match_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSummaryEntity { // 매치 요약 페이지 테이블

    @Id
    private String matchId;
    private String puuid;

    private String championName;
    private String teamPosition;
    private int championLevel;

    private String gameMode;
    private String queueType;

    private boolean win;

    private int kills;
    @Column(name = "kill_participation")
    private Double killParticipation;
    private int deaths;
    private int assists;
    @Column(name = "kda_ratio")
    private Double kdaRatio;
    private int cs;
    private double csPerMin;

    private LocalDateTime gameEndTimestamp;
    private int gameDurationSeconds;

    private String tier;


    @Column(name = "damage_dealt")
    private Integer damageDealt;

    @Column(name = "damage_taken")
    private Integer damageTaken;

    @Column(columnDefinition = "TEXT") // 내용이 길어질 수 있는 경우를 위해 ( 콤마로 구분된 긴 문자열을 저장 )
    private String itemIds;


    @Column(columnDefinition = "TEXT")
    private String traitIds;

    private Integer spell1Id;
    private Integer spell2Id;

    private Integer mainRune1Id;
    private Integer mainRune2Id;

    @Column(columnDefinition = "TEXT")
    private String otherProfileIconIds;

    @Column(columnDefinition = "TEXT")
    private String otherSummonerNames;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.sql.Timestamp createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Timestamp.valueOf(LocalDateTime.now());
    }

    // 상세 전적에서 나의 정보만 추출 -> 요약 엔티티로 변환
    public static MatchSummaryEntity fromDetailDTO(MatchDetailDTO detail, String puuid) {
        RiotParticipantDTO me = detail.getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid()))
                .findFirst()
                .orElse(null);
        if (me == null) return null;

        int kills = me.getKills();
        int deaths = me.getDeaths();
        int assists = me.getAssists();
        int cs = me.getTotalMinionsKilled();
        int duration = detail.getGameDurationSeconds();

        // KDA 계산
        double kdaRatio = (double)(kills + assists) / Math.max(deaths, 1);

        // CS per min 계산
        double csPerMin = duration > 0 ? ((double) cs) / (duration / 60.0) : 0.0;

        String itemIds = me.getItemIds() == null ? "" :
                me.getItemIds().stream().map(String::valueOf).collect(Collectors.joining(","));

        double killParticipation = MatchHelper.calculateKillParticipation(
                kills, assists, me.getTeamTotalKills()
        );

        Map<Integer, Integer> teamKillMap = detail.getParticipants().stream()
                .collect(Collectors.groupingBy(
                        RiotParticipantDTO::getTeamId,
                        Collectors.summingInt(RiotParticipantDTO::getKills)
                ));

        for (RiotParticipantDTO p : detail.getParticipants()) {
            p.setTeamTotalKills(teamKillMap.get(p.getTeamId()));
        }

        return MatchSummaryEntity.builder()
                .matchId(detail.getMatchId())
                .puuid(puuid)
                .queueType(detail.getQueueType())
                .tier(me.getTier())
                .win(me.isWin())
                .teamPosition(me.getTeamPosition())
                .championName(me.getChampionName())
                .kills(kills)
                .killParticipation(killParticipation)
                .deaths(deaths)
                .assists(assists)
                .kdaRatio(kdaRatio)
                .cs(cs)
                .csPerMin(csPerMin)
                .damageDealt(me.getTotalDamageDealtToChampions())
                .damageTaken(me.getTotalDamageTaken())
                .championLevel(me.getChampionLevel())
                .itemIds(itemIds)
                .traitIds(Optional.ofNullable(me.getTraits()).map(l -> String.join(",", l)).orElse(null))
                .otherSummonerNames(
                        Optional.ofNullable(detail.getOtherSummonerNames())
                                .map(list -> String.join(",", list))
                                .orElse(null)
                )
                .gameEndTimestamp(detail.getGameEndTimestamp())
                .gameMode(detail.getGameMode())
                .gameDurationSeconds(duration)
                .spell1Id(me.getSpell1Id())
                .spell2Id(me.getSpell2Id())
                .mainRune1Id(me.getMainRune1())
                .mainRune2Id(me.getMainRune2())
                .otherProfileIconIds(
                        Optional.ofNullable(detail.getOtherProfileIconIds())
                                .map(list -> String.join(",", list))
                                .orElse(null)
                )

                .build();
    }


    // DB 에 저장된 요약정보 -> DTO 변환
    public MatchHistoryDTO toDTO() {
        return MatchHistoryDTO.builder()
                .matchId(this.matchId)
                .win(this.win)
                .teamPosition(this.teamPosition)
                .championName(this.championName)
                .kills(this.kills)
                .killParticipation(this.killParticipation)
                .deaths(this.deaths)
                .assists(this.assists)
                .cs(this.cs)
                .csPerMin(this.csPerMin)
                .kdaRatio(this.kdaRatio)
                .tier(this.tier)
                .gameMode(this.gameMode)
                .gameEndTimestamp(this.gameEndTimestamp)
                .queueType(this.queueType)
                .championLevel(this.championLevel)
                .traitIds(MatchHelper.splitString(this.traitIds))
                .otherSummonerNames(MatchHelper.splitString(this.otherSummonerNames))
                .gameDurationSeconds(this.gameDurationSeconds)
                .build();
    }




}
