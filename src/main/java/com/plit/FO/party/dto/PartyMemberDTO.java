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
    private Long userSeq;
    private LocalDateTime joinTime;
    private String role;
    private String message;
    private MemberStatus status;
    private String position;
    private String userId;

    private String userNickname;            // 닉네임 (user_nickname)
    private String tier;                    // 티어
    private List<String> preferredChampions; // 선호 챔피언
    private Double winRate;                 // 승률
    private Double averageKda;              // KDA

    private List<String> championImageUrls;
    private String tierImageUrl;

    public PartyMemberDTO(PartyMemberEntity entity, Integer userSeq, String userNickname) {
        this.id = entity.getId();
        this.userSeq = userSeq.longValue();
        this.joinTime = entity.getJoinTime();
        this.role = entity.getRole();
        this.message = entity.getMessage();
        this.status = entity.getStatus();
        this.position = entity.getPosition();
        this.userNickname = userNickname;
    }
}