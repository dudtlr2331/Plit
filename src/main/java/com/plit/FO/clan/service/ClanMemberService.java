package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.dto.ClanMemberDTO;

import java.util.List;
import java.util.Optional;

public interface ClanMemberService {
    List<ClanMemberDTO> findApprovedMembersByClanId(Long clanId);
    List<ClanMemberDTO> findPendingMembersByClanId(Long clanId);
    Optional<ClanMemberDTO> findByClanIdAndUserId(Long clanId, Long userId);


}