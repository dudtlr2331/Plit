package com.plit.FO.matchHistory;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO {

    private String matchId;
    private boolean win;
    private String teamPosition;
    private String championName;
    private int kills;
    private int deaths;
    private int assists;

    private String summonerName;
    private String tier;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private LocalDateTime gameEndTimestamp;
    private String gameMode;
    private List<String> itemIds;

}
