package com.plit.FO.matchHistory.dto.db;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchOverallSummaryDTO {
    private Long id;
    private String puuid;
    private String gameName;
    private String tagLine;
    private String tier;
    private int totalMatches;
    private int totalWins;
    private int winCount;
    private int totalCount;
    private double winRate;
    private double averageKills;
    private double averageDeaths;
    private double averageAssists;
    private double averageKda;
    private double averageCs;
    private String preferredPosition;
    private Map<String, Long> positionCounts;
    private List<String> preferredChampions;
    private Map<String, Integer> championTotalGames;
    private Map<String, Integer> championWins;
    private Map<String, Double> championWinRates;
    private Map<String, Double> championKdaRatios;
    private Map<String, Double> favoritePositions;
    private LocalDateTime createdAt;
    private int loseCount;
    private double killParticipation;
    private List<Map.Entry<String, Long>> sortedChampionList;
    private List<String> sortedPositionList;
    private String preferredPositionImageUrl;
    private List<String> favoriteChampionImageUrls;

    private Map<String, Double> rankedFavoritePositions;
    private List<String> rankedSortedPositionList;


    public int getLoseCount() {
        return totalMatches - winCount;
    }

}
