package com.plit.FO.matchHistory.entity;

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
public class MatchPlayerEntity { // 매치 상세페이지 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchId;
    private String puuid;
    private String summonerName;
    private String championName;

    private int kills;
    private int deaths;
    private int assists;
    private double kdaRatio;

    private int cs;
    private double csPerMin;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private String teamPosition;
    private String tier;

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
}
