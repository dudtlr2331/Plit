package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchSummaryRepository extends JpaRepository<MatchSummaryEntity, Long> {

    // 해당 유저의 최근 20게임 match 요약 정보를 gameEndTimestamp 기준으로 내림차순 정렬해서 가져옴
    List<MatchSummaryEntity> findTop20ByPuuidOrderByGameEndTimestampDesc(String puuid);

    // 특정 matchId가 이미 저장되어 있는지 확인
    boolean existsByMatchId(String matchId);

    boolean existsByPuuid(String puuid);

    @Query("SELECT m.matchId FROM MatchSummaryEntity m WHERE m.puuid = :puuid")
    List<String> findMatchIdsByPuuid(@Param("puuid") String puuid);

    List<MatchSummaryEntity> findByPuuid(String puuid);

    // 해당 유저의 전체 매치 데이터를 gameEndTimestamp 기준으로 내림차순 정렬해서 가져옴
    List<MatchSummaryEntity> findByPuuidOrderByGameEndTimestampDesc(String puuid);
}
