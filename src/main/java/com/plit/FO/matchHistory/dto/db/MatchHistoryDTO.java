package com.plit.FO.matchHistory.dto.db;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO { // 최근 전적 리스트 한 줄씩 요약

    private String matchId;
    private boolean win;
    private String teamPosition;
    private String championName;
    private int kills;
    private double killParticipation; // 킬관여
    private int deaths;
    private int assists;
    private int cs;
    private double csPerMin;
    private double kdaRatio;
    private String tier;
    private String championImageUrl;
    private String profileIconUrl;
    private String gameMode;
    private LocalDateTime gameEndTimestamp; // 사용자에게 보여주는 용도라
    private List<String> itemImageUrls;
    private String queueType; // 큐타입

    private String timeAgo;

    private int championLevel;

    private String mainRune1Url;
    private String mainRune2Url;

    private String spell1ImageUrl;
    private String spell2ImageUrl;

    private String tierImageUrl;

    private List<String> traitIds;
    private List<String> traitImageUrls;

    private List<String> otherSummonerNames;

    private List<String> otherProfileIconUrls;

    private int gameDurationSeconds;

    public int getGameDurationMinutes() {
        return gameDurationSeconds / 60;
    }

    public int getGameDurationRemainSeconds() {
        return gameDurationSeconds % 60;
    }

}
