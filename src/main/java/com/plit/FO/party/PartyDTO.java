package com.plit.FO.party;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDTO {
    private Integer partySeq;  // 번호
    private String partyName;  // 파티 이름
    private String partyType;  // 타입
    private LocalDateTime partyCreateDate; // 생성일자
    private LocalDateTime partyEndTime; // 파티 종료 일자
    private String partyStatus; // 파티 상태 (WAITING, FULL, CLOSED 등)
    private Integer partyHeadcount; // 파티 인원 수
    private Integer partyMax; // 최대 모집 인원 수
}
