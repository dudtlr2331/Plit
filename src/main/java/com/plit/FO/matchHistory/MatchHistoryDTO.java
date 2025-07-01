package com.plit.FO.matchHistory;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO { // 매치정보

    //
    private String matchId;
    private boolean win;
    private String teamPosition;
    private String championName;

    private int kills;
    private int deaths;
    private int assists;

    //
    private String summonerName;
    private String tier;

    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    //
    private LocalDateTime gameEndTimestamp;
    private String gameMode;
    private List<String> itemIds;

    //
    private String profileIconUrl;
    private List<String> itemImageUrls;

    private int cs;
    private double csPerMin;
    private int wardsPlaced;
    private int wardsKilled;

    private int gameDurationMinutes;
    private int gameDurationSeconds;
    private String killParticipation;

    private String queueType;

    // 이미지
    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;

    private String championImageUrl;
    private String mainRune1Url;
    private String mainRune2Url;
    private String statRune1Url;
    private String statRune2Url;

    public String getChampionImageUrl() {
        return "/img/champions/" + championName + ".png";
    }
    public String getMainRune1Url() {
        return "/img/runes/" + mainRune1 + ".png";
    }
    public String getMainRune2Url() {
        return "/img/runes/" + mainRune2 + ".png";
    }
    public String getStatRune1Url() {
        return "/img/runes/" + statRune1 + ".png";
    }
    public String getStatRune2Url() {
        return "/img/runes/" + statRune2 + ".png";
    }


}
