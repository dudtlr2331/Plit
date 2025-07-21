package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.MatchOverallSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchOverallSummaryRepository extends JpaRepository<MatchOverallSummaryEntity, Long> {
    Optional<MatchOverallSummaryEntity> findByPuuid(String puuid);
}