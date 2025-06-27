package com.plit.FO.party;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    @Autowired
    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public List<PartyEntity> findAllParties() {
        return partyRepository.findAll();
    }

    public PartyEntity saveParty(PartyEntity party) { // 파티 생성 또는 업데이트 (JPA save 메서드가 insert/update를 자동으로 판단)
        return partyRepository.save(party);
    }

    public PartyEntity getParty(Long id) {
        return partyRepository.findById(id).orElse(null);
    }

    public void deleteParty(Long id) {
        partyRepository.deleteById(id);
    }
}
