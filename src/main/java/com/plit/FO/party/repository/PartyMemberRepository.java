package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMemberEntity, Long> {

    List<PartyMemberEntity> findByParty(PartyEntity party);

    Optional<PartyMemberEntity> findByPartyAndUserId(PartyEntity party, String userId);

    void deleteByParty(PartyEntity party);

    boolean existsByParty_PartySeqAndUserId(Long party, String userId);

    List<PartyMemberEntity> findByParty_PartySeq(Long partySeq);

    int countByParty_PartySeqAndStatus(Long partySeq, MemberStatus status);

    boolean existsByParty_PartySeqAndUserIdAndStatus(Long partySeq, String userId, MemberStatus status);

    List<PartyMemberEntity> findByParty_PartySeqAndStatus(Long partySeq, MemberStatus status);

    Optional<PartyMemberEntity> findByParty_PartySeqAndUserId(Long partyId, String userId);

    boolean existsByParty_PartySeqAndStatusAndPosition(Long partySeq, String status, String position);

}