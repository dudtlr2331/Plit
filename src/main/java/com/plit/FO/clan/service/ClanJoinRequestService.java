package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanJoinRequestDTO;

import java.util.List;

public interface ClanJoinRequestService {

    void requestJoin(ClanJoinRequestDTO dto);

    List<ClanJoinRequestDTO> getJoinRequests(Long clanId);

    boolean isJoinPending(Long clanId, Long userId);

    void approveJoinRequest(Long clanId, Long userId);

    void rejectJoinRequest(Long clanId, Long userId);

}