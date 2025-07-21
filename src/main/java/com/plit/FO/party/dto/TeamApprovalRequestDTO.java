package com.plit.FO.party.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TeamApprovalRequestDTO {
    private List<Long> memberIds;
}
