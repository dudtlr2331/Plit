package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.dto.ClanMemberDTO;

import java.util.List;
import java.util.Optional;

public interface ClanMemberService {
    List<ClanMemberDTO> findApprovedMembersByClanId(Long clanId);

    List<ClanMemberDTO> findPendingMembersByClanId(Long clanId);

    Optional<ClanMemberDTO> findByClanIdAndUserId(Long clanId, Long userId);

    void updateMemberInfo(Long userId, Long clanId, ClanMemberDTO dto);

    int countByClanId(Long clanId);

    void addMember(Long clanId, Long userId, String position, String tier, String intro);

    void delegateLeader(Long clanId, Long fromUserSeq, Long toUserSeq);

    void kickMember(Long clanId, Long requesterUserSeq, Long targetUserSeq);

    void leaveClan(Long clanId, Long userSeq);

    int countPendingMembers(Long clanId);

}
