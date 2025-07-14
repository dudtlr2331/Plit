package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanJoinRequestDTO;

import java.util.List;

public interface ClanJoinRequestService {

    void requestJoin(ClanJoinRequestDTO dto); // 가입 신청

    List<ClanJoinRequestDTO> getJoinRequests(Long clanId); // 수락 대기 목록 조회

    boolean isJoinPending(Long clanId, Long userId);

    void approveJoinRequest(Long clanId, Long userId);

    void rejectJoinRequest(Long clanId, Long userId);

}