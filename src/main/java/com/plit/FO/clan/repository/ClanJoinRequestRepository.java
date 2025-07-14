package com.plit.FO.clan.repository;

import com.plit.FO.clan.entity.ClanJoinRequestEntity;
import com.plit.FO.clan.enums.JoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClanJoinRequestRepository extends JpaRepository<ClanJoinRequestEntity, Long> {
    List<ClanJoinRequestEntity> findByClan_Id(Long clanId);

    boolean existsByClanIdAndUserIdAndStatus(Long clanId, Long userId, JoinStatus status);

    Optional<ClanJoinRequestEntity> findByClanIdAndUserIdAndStatus(Long clanId, Long userId, JoinStatus status);

    List<ClanJoinRequestEntity> findByClan_IdAndStatus(Long clanId, JoinStatus status);

}

