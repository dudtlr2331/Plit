package com.plit.FO.matchHistory.dto.riot;

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
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;
    private String teamPosition;
    private boolean win;
    private int teamId;

    private int champLevel;
    private int summoner1Id;
    private int summoner2Id;
    private List<Integer> itemIds;

    private int profileIcon;
    private int totalMinionsKilled;
    private int neutralMinionsKilled;

    private int perkPrimaryStyle;
    private int perkSubStyle;
    private String individualPosition;

}
