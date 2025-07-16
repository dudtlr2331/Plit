package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotMatchInfoDTO { // info 필드
    private long gameEndTimestamp; // 원본 정보 그대로
    private String gameMode;
    private List<RiotParticipantDTO> participants;

    private int gameDurationSeconds;

    private String queueId;


}
