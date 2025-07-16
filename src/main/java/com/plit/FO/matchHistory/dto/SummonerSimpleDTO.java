package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummonerSimpleDTO {
    private String puuid;
    private String gameName;
    private String tagLine;
    private Integer profileIconId;
    private String profileIconUrl;
    private Integer summonerLevel;
}
