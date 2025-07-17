package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchPlayerDTO { // 매치 각각 상세정보 -> 소환사 1명의 정보

    private String puuid;
    private String matchId; // 하나의 게임 고유 ID
    private boolean win;
    private int teamId;
    private String teamPosition;

    // 기본 정보
    private String championName;
    private String championKorName;
    private int championLevel;
    private String summonerName;
    private String tier;
    private int profileIconId;
    private String profileIconUrl;

    // 전투 정보
    private int kills;
    private int deaths;
    private int assists;

    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private int cs;
    private double csPerMin;

    private String killParticipation;

    private double kdaRatio;

    private String badge;

    private int goldEarned;

    // 시간
    private LocalDateTime gameEndTimestamp;
    private int gameDurationMinutes;
    private int gameDurationSeconds;
    private String timeAgo;

    // 게임 종류
    private String gameMode;
    private String queueType;

    // 시야 - 와드
    private int wardsPlaced;
    private int wardsKilled;

    // 아이템
    private List<String> itemIds;
    private List<String> itemImageUrls;

    // 룬
    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;

    private String mainRune1Url;
    private String mainRune2Url;
    private String statRune1Url;
    private String statRune2Url;

    // 챔피언 이미지 경로
    private String championImageUrl;

    // 스펠 이미지 경로
    private String spell1ImageUrl;
    private String spell2ImageUrl;

    // 티어 이미지 경로
    private String tierImageUrl;

    public static MatchPlayerDTO fromRiotParticipant(RiotParticipantDTO p) {
        return MatchPlayerDTO.builder()
                .puuid(p.getPuuid())
                .summonerName(p.getSummonerName())
                .championName(p.getChampionName())
                .tier(p.getTier())
                .kills(p.getKills())
                .deaths(p.getDeaths())
                .assists(p.getAssists())
                .goldEarned(p.getGoldEarned())
                .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                .totalDamageTaken(p.getTotalDamageTaken())
                .teamPosition(p.getTeamPosition())
                .win(p.isWin())
                .teamId(p.getTeamId())
                .championLevel(p.getChampLevel())
                .itemIds(p.getItemIds().stream().map(String::valueOf).toList())
                .profileIconId(p.getProfileIcon())
                .cs(p.getTotalMinionsKilled() + p.getNeutralMinionsKilled())
                .mainRune1(p.getPerkPrimaryStyle())
                .mainRune2(p.getPerkSubStyle())
                .build();
    }


}
