package com.plit.FO.clan.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClanJoinRequestDTO {

    private Long clanId;         // 가입할 클랜 ID
    private Long userId;         // 로그인한 유저 ID
    private String nickname;     // 보여줄 닉네임
    private String tier;         // 티어 표시용
    private String mainPosition; // 선택한 주 포지션
    private String intro;        // 자기소개
    private LocalDateTime requestAt;
}