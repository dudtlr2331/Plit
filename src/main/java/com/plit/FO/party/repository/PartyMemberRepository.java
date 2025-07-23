package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMemberEntity, Long> {

    boolean existsByParty_PartySeqAndUser_UserId(Long party, String userId);

    List<PartyMemberEntity> findByParty_PartySeq(Long partySeq);

    int countByParty_PartySeqAndStatus(Long partySeq, MemberStatus status);

    boolean existsByParty_PartySeqAndUser_UserIdAndStatus(Long partySeq, String userId, MemberStatus status);

    List<PartyMemberEntity> findByParty_PartySeqAndStatus(Long partySeq, MemberStatus status);

    Optional<PartyMemberEntity> findByParty_PartySeqAndUser_UserId(Long partyId, String userId);

    boolean existsByParty_PartySeqAndStatusAndPosition(Long partySeq, String status, String position);

    @Query("SELECT pm FROM PartyMemberEntity pm JOIN FETCH pm.user WHERE pm.party.partySeq = :partySeq")
    List<PartyMemberEntity> findWithUserByPartySeq(@Param("partySeq") Long partySeq);

}