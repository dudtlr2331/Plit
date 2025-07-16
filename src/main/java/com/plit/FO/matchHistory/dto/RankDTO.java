package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankDTO { // 랭크 정보

    // 소환사 정보
    private String gameName;
    private String tagLine;
    private int summonerLevel;
    private int profileIconId;

    // 랭크 정보
    private String tier;
    private String rank;
    private int leaguePoints; // LP
    private int wins;
    private int losses;
    private double winRate;

    private String tierImageUrl;

    public String getTierRankString() {
        return tier + " " + rank;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public String getWinRateString() {
        return String.format("%.1f%%", winRate);
    }

}
