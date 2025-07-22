package com.plit.FO.clan.dto;

import com.plit.FO.clan.enums.Position;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClanJoinRequestDTO {

    private Long userId;
    private String nickname;
    private String profileIconUrl;
    private String tier;
    private String tierShort;
    private String tierImageUrl;
    private String role;
    private String status;
    private boolean isAdmin;
    private boolean adminUser;

    private Double winRate;
    private Double averageKills;
    private Double averageDeaths;
    private Double averageAssists;
    private Double averageKda;
    private Integer totalWins;
    private Integer totalLosses;
    private String preferredChampions;
    private List<String> championImageUrls;

    private Long clanId;
    private Long memberId;
    private Position position;
    private String intro;

    private LocalDateTime requestAt;
    private LocalDateTime joinedAt;
    private String joinedAgo;

}