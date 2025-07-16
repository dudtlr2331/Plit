package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotAccountResponse { // 처음 riot id -> puuid 매핑
    private String puuid;
    private String gameName;
    private String tagLine;
}
