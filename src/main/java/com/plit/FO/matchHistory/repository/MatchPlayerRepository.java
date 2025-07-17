package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayerEntity, Long> {

    // 매치 ( 하나의 matchId ) 안의 플레이어 정보 전부
    List<MatchPlayerEntity> findByMatchId(String matchId);
}
