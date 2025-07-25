package com.plit.FO.matchHistory.dto;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSummaryDTO { //  매치 전체( 20게임 ) 요악 정보 - DB 저장 x

    // 전체 전적 요약
    private int winCount;
    private int loseCount;
    private int totalCount;
    private double averageKills;
    private double averageDeaths;
    private double averageAssists;
    private double averageKda;
    private double kdaRatio;
    private double killParticipation;

    // 포지션별 통계
    private Map<String, Integer> positionTotalGames;
    private Map<String, Integer> positionWins;
    private Map<String, Double> positionWinRates;
    private Map<String, Double> favoritePositions;

    // 챔피언별 통계
    private Map<String, Integer> championTotalGames;
    private Map<String, Integer> championWins;
    private Map<String, Double> championWinRates;
    private Map<String, Double> championKdaRatios = new HashMap<>();
    private List<Map.Entry<String, Integer>> sortedChampionList;

    // 상위 챔피언 정보
    private List<FavoriteChampionDTO> topChampions;

    private List<String> sortedPositionList;

}
