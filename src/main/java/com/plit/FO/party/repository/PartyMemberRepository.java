package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMemberEntity, Long> {

    List<PartyMemberEntity> findByParty(PartyEntity party);

    Optional<PartyMemberEntity> findByPartyAndUserId(PartyEntity party, String userId);

    void deleteByParty(PartyEntity party);

    boolean existsByParty_PartySeqAndUserId(Long party, String userId);

    List<PartyMemberEntity> findByParty_PartySeq(Long partySeq);
}