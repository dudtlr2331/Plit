package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotAccountResponse { // 처음 riot id -> puuid 매핑 // riot 이 응답 이름에 response 라는 이름을 많이 씀
    private String puuid;
    private String gameName;
    private String tagLine;
}
