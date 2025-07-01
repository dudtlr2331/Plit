package com.plit.FO.matchHistory.test_cache;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MatchHistoryRepository extends JpaRepository<MatchHistoryEntity, Long> {
    Optional<MatchHistoryEntity> findByGameNameAndTagLine(String gameName, String tagLine);
}


