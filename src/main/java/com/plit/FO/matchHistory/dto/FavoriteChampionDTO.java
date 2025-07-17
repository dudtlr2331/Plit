package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteChampionDTO { // 선호챔피언

    private String championName;
    private String korName;

    private int totalCs;
    private int averageCs;
    private double csPerMin;

    private double kills;
    private double deaths;
    private double assists;

    private int gameCount;
    private int winCount;
    private double winRate;

    private double kdaRatio;

    private double flexPickRate;
    private int flexGames;

    private String championImageUrl;

    private Integer flexGameCount;
    private Double flexUsagePercent;

}
