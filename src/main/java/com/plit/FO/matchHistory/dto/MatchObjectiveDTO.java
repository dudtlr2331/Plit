package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchObjectiveDTO {
    private int totalKills;
    private int totalGold;
    private int towerKills; // 타워
    private int dragonKills; // 드래곤
    private int baronKills; // 바론
    private int heraldKills; //
    private int riftKills; //
}
