package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMemberEntity, Long> {

    List<PartyMemberEntity> findByParty(PartyEntity party);

    Optional<PartyMemberEntity> findByPartyAndUserId(PartyEntity party, Long userId);

    void deleteByParty(PartyEntity party);
}