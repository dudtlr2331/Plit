package com.plit.FO.party.dto;

import com.plit.FO.party.entity.PartyMemberEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyMemberDTO {
    private Long id;
    private String userId;
    private LocalDateTime joinTime;
    private String role;
    private String message;
    private String status;

    public PartyMemberDTO(PartyMemberEntity entity) {
        this.id = entity.getId();
        this.userId = entity.getUserId();
        this.joinTime = entity.getJoinTime();
        this.role = entity.getRole();
        this.message = entity.getMessage();
        this.status = entity.getStatus();
    }
}