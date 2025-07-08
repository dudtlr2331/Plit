package com.plit.FO.clan.repository;

import com.plit.FO.clan.entity.ClanMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanMemberRepository extends JpaRepository<ClanMemberEntity, Long> {

    // 특정 유저가 클랜 멤버인지 여부 확인
    boolean existsByClanIdAndUserId(Long clanId, Long userId);

    // 필요 시, 수락된 멤버만 조회하거나 필터할 수도 있어
    // List<ClanMemberEntity> findByClanIdAndStatus(Long clanId, String status);
}