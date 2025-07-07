package com.plit.FO.party.service;

import com.plit.FO.party.dto.PartyDTO;
import java.util.List;

public interface PartyService {
    List<PartyDTO> findByPartyType(String partyType);
    PartyDTO getParty(Long id);
    void saveParty(PartyDTO dto, String userId);
    void updateParty(Long id, PartyDTO dto);
    void deleteParty(Long id);

    void joinParty(Long partySeq, String userId);
}