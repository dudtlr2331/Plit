package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotMatchMetadataDTO { // metadata 필드 - 어떤 경기인지 / 참가자 10명 식별

    // 상세 페이지에서 10명 닉네임 + 아이콘 보여줄 때

    // 매치 중 다른 소환사의 puuid를 활용해 추가 전적 조회할 때

    private String matchId;
    private List<String> participants;
}
