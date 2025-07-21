package com.plit.FO.party.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrimJoinRequestDTO {
    private List<ScrimMemberDTO> teamMembers;
    private String message;
}
