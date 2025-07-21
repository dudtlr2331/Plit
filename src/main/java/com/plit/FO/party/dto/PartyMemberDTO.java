package com.plit.FO.party.dto;

import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.enums.MemberStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private MemberStatus status;
    private String position;
    private Integer userSeq;

    private String userNickname;
    private String tier;                     // 티어 (예: "Unranked" 또는 추후 등급 시스템)
    private List<String> preferredChampions; // 선호 챔피언 (최대 3개)
    private Double winRate;                  // 승률 (예: 55.6)
    private Double averageKda;               // KDA (예: 3.25)

    public PartyMemberDTO(PartyMemberEntity entity, Integer userSeq) {
        this.id = entity.getId();
        this.userId = entity.getUserId();
        this.joinTime = entity.getJoinTime();
        this.role = entity.getRole();
        this.message = entity.getMessage();
        this.status = entity.getStatus();
        this.position = entity.getPosition();
        this.userSeq = userSeq;
    }

    public PartyMemberDTO(PartyMemberEntity entity, Integer userSeq, String userNickname) {
        this.id = entity.getId();
        this.userId = entity.getUserId();
        this.joinTime = entity.getJoinTime();
        this.role = entity.getRole();
        this.message = entity.getMessage();
        this.status = entity.getStatus();
        this.position = entity.getPosition();
        this.userSeq = userSeq;
        this.userNickname = userNickname;
    }
}