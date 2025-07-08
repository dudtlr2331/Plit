package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyFindPositionEntity;
import com.plit.FO.party.entity.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyFindPositionRepository extends JpaRepository<PartyFindPositionEntity, Long> {
    List<PartyFindPositionEntity> findByParty(PartyEntity party);
    void deleteByParty(PartyEntity party);
}
