package com.plit.FO.matchHistory;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankDTO {
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;
    private double winRate;

    private int highestLp;
    private String highestTier;

}
