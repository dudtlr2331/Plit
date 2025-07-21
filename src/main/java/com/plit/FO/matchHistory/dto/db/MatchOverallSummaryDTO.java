package com.plit.FO.matchHistory.dto.db;

import lombok.*;

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
    private Map<String, Integer> favoritePositions;
    private LocalDateTime createdAt;
    private int loseCount;
    private double killParticipation;



    public int getLoseCount() {
        return totalMatches - winCount;
    }



}
