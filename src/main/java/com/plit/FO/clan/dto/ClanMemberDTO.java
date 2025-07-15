package com.plit.FO.clan.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ClanMemberDTO {
    private Long memberId;
    private String userId;
    private String nickname;
    private String tier;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private String mainPosition;
    private String intro;
    private String tag;
    private Long clanId;
    private String joinedAgo;
}