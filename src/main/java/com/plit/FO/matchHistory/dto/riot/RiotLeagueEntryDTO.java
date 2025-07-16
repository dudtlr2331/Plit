package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotLeagueEntryDTO { // 티어 정보용
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;
}
