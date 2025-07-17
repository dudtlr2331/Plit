package com.plit.FO.matchHistory.dto.riot;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiotSummonerResponse {
    private Integer profileIconId;
    private Integer summonerLevel;
}
