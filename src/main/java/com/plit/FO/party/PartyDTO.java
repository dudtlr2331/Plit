package com.plit.FO.party;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDTO {
    private Long partySeq;  // 번호
    private String partyName;  // 파티 이름
    private String partyType;  // 타입
    private LocalDateTime partyCreateDate; // 생성일자
    private LocalDateTime partyEndTime; // 파티 종료 일자
    private String partyStatus; // 파티 상태 (WAITING, FULL, CLOSED 등)
    private Integer partyHeadcount; // 파티 인원 수
    private Integer partyMax; // 최대 모집 인원 수
    private String memo; //메모
    private String mainPosition; //주 포지션
    private List<PositionEnum> positions = new ArrayList<>(); // 찾는 포지션

    public PartyDTO(PartyEntity entity) {
        this.partySeq = entity.getPartySeq();
        this.partyName = entity.getPartyName();
        this.partyType = entity.getPartyType();
        this.partyCreateDate = entity.getPartyCreateDate();
        this.partyEndTime = entity.getPartyEndTime();
        this.partyStatus = entity.getPartyStatus();
        this.partyHeadcount = entity.getPartyHeadcount();
        this.partyMax = entity.getPartyMax();
        this.memo = entity.getMemo();
        this.mainPosition = entity.getMainPosition();
        this.positions = entity.getPartyFindPositions().stream()
                .map(PartyFindPositionEntity::getPosition)
                .toList();
    }
}
