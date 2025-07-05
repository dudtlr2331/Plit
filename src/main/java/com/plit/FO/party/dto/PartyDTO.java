package com.plit.FO.party.dto;

import com.plit.FO.party.entity.PartyEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.plit.FO.party.entity.PartyFindPositionEntity;

import java.util.stream.Collectors;

import com.plit.FO.party.entity.PartyEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDTO {
    private Long partySeq;
    private String partyName;
    private String partyType;
    private LocalDateTime partyCreateDate;
    private LocalDateTime partyEndTime;
    private String partyStatus;
    private Integer partyHeadcount;
    private Integer partyMax;
    private String memo;
    private String mainPosition;
    private String  createdBy;

    private List<String> positions;
    private List<PartyMemberDTO> members;

    public PartyDTO(PartyEntity entity) {
        this.partySeq = entity.getPartySeq();
        this.partyName = entity.getPartyName();
        this.partyType = entity.getPartyType();
        this.partyCreateDate = entity.getPartyCreateDate();
        this.partyEndTime = entity.getPartyEndTime();
        this.partyStatus = entity.getPartyStatus().name();
        this.partyHeadcount = entity.getPartyHeadcount();
        this.partyMax = entity.getPartyMax();
        this.memo = entity.getMemo();
        this.mainPosition = entity.getMainPosition();
        this.createdBy = entity.getCreatedBy();

        this.positions = entity.getPartyFindPositions().stream()
                .map(p -> p.getPosition().name())
                .collect(Collectors.toList());

        this.members = entity.getPartyMembers().stream()
                .map(PartyMemberDTO::new)
                .collect(Collectors.toList());
    }
}