package com.plit.FO.matchHistory.dto.db;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMatchSummaryDTO {
    private String tier;
    private List<String> preferredChampions;
    private double winRate;
    private double averageKills;
    private double averageDeaths;
    private double averageAssists;
    private double averageKda;
}
