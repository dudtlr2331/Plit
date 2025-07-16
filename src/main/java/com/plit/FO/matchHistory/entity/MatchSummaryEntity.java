package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private LocalDateTime gameEndTimestamp;

    private String gameMode;

    private boolean win;

    private String championName;

    private String teamPosition;

    private int kills;
    private int deaths;
    private int assists;

    private int champLevel;
    private int cs;

    private double kda;
    @Column(name = "kda_ratio")
    private Double kdaRatio;

    private String tier;

    @Column(columnDefinition = "TEXT")
    private String itemIds;

    @Column(columnDefinition = "TEXT")
    private String itemImageUrls;

    @Column(columnDefinition = "TEXT")
    private String traitIds;

    @Column(columnDefinition = "TEXT")
    private String traitImageUrls;

    @Column(columnDefinition = "TEXT")
    private String otherSummonerNames;

    @Column(columnDefinition = "TEXT")
    private String otherProfileIconUrls;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.sql.Timestamp createdAt;

    private double csPerMin;
    private String championImageUrl;
    private String profileIconUrl;

    private String queueType;
    private String timeAgo;

    private String mainRune1Url;
    private String mainRune2Url;
    private String spell1ImageUrl;
    private String spell2ImageUrl;
    private String tierImageUrl;

    private int gameDurationSeconds;


    public static MatchSummaryEntity fromDetailDTO(MatchDetailDTO detail, String puuid) {
        RiotParticipantDTO me = detail.getParticipants().stream()
                .filter(p -> puuid.equals(p.getPuuid()))
                .findFirst()
                .orElse(null);
        if (me == null) return null;

        return MatchSummaryEntity.builder()
                .matchId(detail.getMatchId())
                .puuid(puuid)
                .win(me.isWin())
                .teamPosition(me.getTeamPosition())
                .championName(me.getChampionName())
                .kills(me.getKills())
                .deaths(me.getDeaths())
                .assists(me.getAssists())
                .kdaRatio((double)(me.getKills() + me.getAssists()) / Math.max(me.getDeaths(), 1))
                .gameEndTimestamp(detail.getGameEndTimestamp())
                .gameMode(detail.getGameMode())
                .champLevel(me.getChampLevel())
                .cs(me.getTotalMinionsKilled())
                .itemIds("")
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
    }

    public MatchHistoryDTO toDTO() {
        return MatchHistoryDTO.builder()
                .matchId(this.matchId)
                .win(this.win)
                .teamPosition(this.teamPosition)
                .championName(this.championName)
                .kills(this.kills)
                .deaths(this.deaths)
                .assists(this.assists)
                .cs(this.cs)
                .csPerMin(this.csPerMin)
                .kdaRatio(this.kdaRatio)
                .tier(this.tier)
                .championImageUrl(this.championImageUrl)
                .profileIconUrl(this.profileIconUrl)
                .gameMode(this.gameMode)
                .gameEndTimestamp(this.gameEndTimestamp)
                .itemImageUrls(splitString(this.itemImageUrls))
                .queueType(this.queueType)
                .timeAgo(this.timeAgo)
                .championLevel(this.champLevel)
                .mainRune1Url(this.mainRune1Url)
                .mainRune2Url(this.mainRune2Url)
                .spell1ImageUrl(this.spell1ImageUrl)
                .spell2ImageUrl(this.spell2ImageUrl)
                .tierImageUrl(this.tierImageUrl)
                .traitIds(splitString(this.traitIds))
                .traitImageUrls(splitString(this.traitImageUrls))
                .otherSummonerNames(splitString(this.otherSummonerNames))
                .otherProfileIconUrls(splitString(this.otherProfileIconUrls))
                .gameDurationSeconds(this.gameDurationSeconds)
                .build();
    }

    private List<String> splitString(String str) {
        return (str != null && !str.isBlank())
                ? Arrays.asList(str.split(","))
                : new ArrayList<>();
    }


}
