package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayerEntity, Long> {

    List<MatchPlayerEntity> findByMatchId(String matchId);
}
