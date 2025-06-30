package com.plit.FO.matchHistory;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteChampionDTO {
    private String name;
    private String korName;
    private double kills;
    private double deaths;
    private double assists;
    private double kdaRatio;
    private int totalCs;
    private double csPerMin;
    private double flexUsagePercent;
    private int flexGameCount;

}
