package com.plit.FO.matchHistory.dto;

import com.plit.FO.matchHistory.entity.FavoriteChampionEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteChampionDTO { // 선호챔피언

    private String puuid;
    private String championName;
    private String korName;
    private String seasonName;
    private String queueType;
    private int gamesPlayed;
    private int wins;

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

    public static FavoriteChampionDTO fromEntity(FavoriteChampionEntity entity) {
        return FavoriteChampionDTO.builder()
                .puuid(entity.getPuuid())
                .championName(entity.getChampionName())
                .queueType(entity.getQueueType())
                .gameCount(entity.getPlayCount())
                .winCount(entity.getWinCount())
                .winRate(entity.getWinRate())
                .kdaRatio(entity.getKdaRatio())
                .kills(entity.getAverageKills())
                .deaths(entity.getAverageDeaths())
                .assists(entity.getAverageAssists())
                .averageCs(entity.getAverageCs() != null ? entity.getAverageCs().intValue() : 0)
                .csPerMin(entity.getCsPerMin() != null ? entity.getCsPerMin() : 0.0)
                .championImageUrl(entity.getChampionImageUrl())
                .build();
    }

}
