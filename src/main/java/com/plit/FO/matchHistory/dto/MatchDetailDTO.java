package com.plit.FO.matchHistory.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailDTO { // 최근 매치 상세 정보 전체 유저

    private String matchId;
    private int totalMaxDamage;

    private List<MatchPlayerDTO> blueTeam;
    private List<MatchPlayerDTO> redTeam;

    private MatchObjectiveDTO blueObjectives;
    private MatchObjectiveDTO redObjectives;
    private boolean blueWin;

}