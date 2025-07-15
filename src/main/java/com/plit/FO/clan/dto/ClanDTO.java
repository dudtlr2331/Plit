package com.plit.FO.clan.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClanDTO {
    private Long id;
    private String name;
    private String intro;
    private String kakaoLink;
    private String discordLink;
    private String minTier;
    private String imageUrl;
    private Long leaderId;
    private int memberCount;
}