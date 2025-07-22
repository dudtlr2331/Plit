package com.plit.FO.clan.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import com.plit.FO.clan.enums.Position;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ClanMemberDTO {
    private String userId;
    private String nickname;
    private String tag;
    private String profileIconUrl;
    private String tier;
    private String tierImageUrl;
    private String tierShort;
    private String puuid;

    private Double winRate;
    private Double averageKills;
    private Double averageDeaths;
    private Double averageAssists;
    private Double averageKda;
    private Integer totalWins;
    private Integer totalLosses;
    private String preferredChampions;
    private List<String> championImageUrls;

    private Long memberId;
    private Long clanId;
    private Position position;
    private String role;
    private String status;
    private String intro;
    private LocalDateTime joinedAt;
    private String joinedAgo;

    private boolean isAdmin;
    private boolean adminUser;
}