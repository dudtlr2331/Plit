package com.plit.FO.matchHistory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.sql.Timestamp createdAt;

}
