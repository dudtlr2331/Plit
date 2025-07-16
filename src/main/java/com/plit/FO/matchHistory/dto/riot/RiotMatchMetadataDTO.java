package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotMatchMetadataDTO { // 대용량 데이터..
    private String matchId;
    private List<String> participants;
}
