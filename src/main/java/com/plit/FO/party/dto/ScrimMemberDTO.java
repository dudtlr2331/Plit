package com.plit.FO.party.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrimMemberDTO {
    private String userNickname;   // 닉네임
    private String position; // TOP, JUNGLE 등
    private String userId;
}
