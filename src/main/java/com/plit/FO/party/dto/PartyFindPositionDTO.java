package com.plit.FO.party.dto;

import com.plit.FO.party.entity.PartyFindPositionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyFindPositionDTO {

    private String position;

    public PartyFindPositionDTO(PartyFindPositionEntity entity) {
        this.position = entity.getPosition().name();
    }
}
