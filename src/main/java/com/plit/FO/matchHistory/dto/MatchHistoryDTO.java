package com.plit.FO.matchHistory.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO { // 최근 전적 리스트 한 줄씩 요약

    private String matchId;
    private boolean win;
    private String teamPosition;
    private String championName;
    private int kills;
    private double killParticipation; // 킬관여
    private int deaths;
    private int assists;
    private int cs;
    private double csPerMin;
    private double kdaRatio;
    private String tier;
    private String championImageUrl;
    private String profileIconUrl;
    private String gameMode;
    private LocalDateTime gameEndTimestamp;
    private List<String> itemImageUrls;
    private String queueType; // 큐타입

}
