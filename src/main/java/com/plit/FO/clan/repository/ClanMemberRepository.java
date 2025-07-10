package com.plit.FO.clan.repository;

import com.plit.FO.clan.entity.ClanMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClanMemberRepository extends JpaRepository<ClanMemberEntity, Long> {

    // status 기반
    List<ClanMemberEntity> findByClanIdAndStatus(Long clanId, String status);  // "APPROVED", "PENDING"

    boolean existsByClanIdAndUserId(Long clanId, Long userId);

    Optional<ClanMemberEntity> findByClanIdAndUserId(Long clanId, Long userId);

    int countByClanId(Long clanId);
}