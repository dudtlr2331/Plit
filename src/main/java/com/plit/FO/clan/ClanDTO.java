package com.plit.FO.clan;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClanDTO {
    private Long id;               // 클랜 ID
    private String name;           // 클랜 이름
    private String intro;          // 소개글
    private String kakaoLink;      // 카카오톡 링크
    private String discordLink;    // 디스코드 링크
    private String minTier;        // 최소 티어
}