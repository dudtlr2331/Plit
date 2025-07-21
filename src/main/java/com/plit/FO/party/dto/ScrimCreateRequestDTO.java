package com.plit.FO.party.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrimCreateRequestDTO {
    private String partyName;
    private LocalDateTime partyEndTime;
    private String memo;
    private List<ScrimMemberDTO> teamMembers;
}
