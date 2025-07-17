package com.plit.FO.matchHistory.dto.riot;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotMatchInfoDTO { // match - info 필드 // 원본 정보 그대로

    private List<RiotParticipantDTO> participants;

    public RiotParticipantDTO getParticipantByPuuid(String puuid) {
        return participants.stream()
                .filter(p -> p.getPuuid().equals(puuid))
                .findFirst()
                .orElse(null);
    }

    private String queueType;
    private String queueId;
    private String gameMode;

    private int gameDurationSeconds;
    private long gameEndTimestamp;

}
