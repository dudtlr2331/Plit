package com.plit.FO.matchHistory.dto.riot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotParticipantDTO { // info 안의 participants 리스트 요소 ( 10명의 소환사 각각의 정보 )

    private String puuid;
    private String summonerName;
    private String championName;
    private String tier;
    private int kills;
    private int deaths;
    private int assists;
    private int goldEarned;
    private int teamTotalKills;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;
    private String teamPosition;
    private boolean win;
    private int teamId;

    private int wardsPlaced;
    private int wardsKilled;

    private int summoner1Id;
    private int summoner2Id;
    private List<Integer> itemIds;
    private List<String> traits;
    private int spell1Id;
    private int spell2Id;
    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;

    @JsonProperty("profileIcon")
    private Integer profileIconId;
    private int totalMinionsKilled;
    private int neutralMinionsKilled;

    private int perkPrimaryStyle;
    private int perkSubStyle;
    private String individualPosition;

}
