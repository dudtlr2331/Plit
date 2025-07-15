package com.plit.FO.clan.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClanJoinRequestDTO {

    private Long clanId;
    private Long userId;
    private String nickname;
    private String tier;
    private String mainPosition;
    private String intro;
    private LocalDateTime requestAt;
}