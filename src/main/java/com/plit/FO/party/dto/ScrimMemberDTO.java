package com.plit.FO.party.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrimMemberDTO {
    private String userId;   // 닉네임
    private String position; // TOP, JUNGLE 등
}
