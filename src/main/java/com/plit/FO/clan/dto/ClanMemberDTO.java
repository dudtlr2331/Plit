package com.plit.FO.clan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClanMemberDTO {
    private Long memberId;
    private String nickname;
    private String tier;           // 티어 정보
    private String role;           // LEADER 또는 MEMBER
    private String status;         // APPROVED, PENDING 등
    private LocalDateTime joinedAt; // 가입 일시
    private String mainPosition;   // 주 포지션 (TOP, MID 등)
    private String intro;          // 소개글
    private String tag;
}