package com.plit.FO.matchHistory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummonerDTO { // 소환사 정보

    private String puuid; // Riot api 유일 식별자
    private String gameName; // 닉네임
    private String tagLine; // # 태그
    private Integer profileIconId; // DB 저장용
    private String profileIconUrl; // 이미지 url

}
