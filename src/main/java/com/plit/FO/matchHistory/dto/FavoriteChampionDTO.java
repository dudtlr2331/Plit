package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteChampionDTO {

    private String championName;
    private String korName;

    private int totalCs;
    private int averageCs;
    private double csPerMin;

    private double kills;
    private double deaths;
    private double assists;

    private double kdaRatio;

    private double flexPickRate;
    private int flexGames;

    private String championImageUrl;

    private Integer flexGameCount;
    private Double flexUsagePercent;

}
