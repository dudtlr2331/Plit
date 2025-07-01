package com.plit.FO.matchHistory;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailDTO { // 최근 매치 상세 정보

    private String matchId;
    private int totalMaxDamage;
    private List<MatchHistoryDTO> blueTeam;
    private List<MatchHistoryDTO> redTeam;

}