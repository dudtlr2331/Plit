package com.plit.FO.matchHistory.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchPlayerDTO { // 매치 각각 상세정보

    private String matchId;
    private boolean win;
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

    private String badge;

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

}
