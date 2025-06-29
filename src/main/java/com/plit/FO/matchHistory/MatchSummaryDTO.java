package com.plit.FO.matchHistory;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSummaryDTO { // 매치 20게임 통계
    private double avgKills;
    private double avgDeaths;
    private double avgAssists;
    private double kdaRatio;
    private double killParticipation;
    private int winCount;
    private int totalCount;

    // 챔피언
    private Map<String, Integer> favoriteChampions;
    private Map<String, Integer> championWinRates;
    private Map<String, Integer> championTotalGames;
    private Map<String, Integer> championWins;
    private List<Map.Entry<String, Integer>> sortedChampionList;

    // 포지션
    private LinkedHashMap<String, Integer> favoritePositions;
    private Map<String, Integer> positionTotalGames;
    private Map<String, Integer> positionWins;
    private Map<String, Integer> positionWinRates;
    private List<Map.Entry<String, Integer>> sortedPositionList;

}
