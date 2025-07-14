package com.plit.FO.party.service;

import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.dto.PartyMemberDTO;
import com.plit.FO.party.enums.MemberStatus;

import java.util.List;

public interface PartyService {
    List<PartyDTO> findByPartyType(String partyType);
    PartyDTO getParty(Long id);
    void saveParty(PartyDTO dto, String userId);
    void updateParty(Long id, PartyDTO dto);
    void deleteParty(Long id);

    String tryJoinParty(Long partySeq, String username);
    List<String> getPartyMembers(Long partySeq);
    void joinParty(Long partyId, String username, String position, String message);
    void acceptMember(Long partyId, Long memberId);
    void rejectMember(Long partyId, Long memberId);
    List<PartyMemberDTO> getPartyMemberDTOs(Long partySeq);
    MemberStatus getJoinStatus(Long partyId, String userId);
    boolean existsByParty_PartySeqAndStatusAndPosition(Long partyId, String status, String position);
    void kickMember(Long partyId, Long memberId, String requesterId);
    void leaveParty(Long partyId, String userId);
}