package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// JpaRepository<RiotIdCacheEntity, Long> : riot_id_cache 테이블에 대한 기본적인 CRUD 제공
// findAll(), save(), findById() ...
public interface RiotIdCacheRepository extends JpaRepository<RiotIdCacheEntity, Long> {

    // 메서드 이름 기반 -> JPA 자동 쿼리 만들어줌 ( 원본용 - 대소문자 구분 )
    Optional<RiotIdCacheEntity> findByGameNameAndTagLine(String gameName, String tagLine);

    // 정규화된 값 - 검색/비교용
    Optional<RiotIdCacheEntity> findByNormalizedGameNameAndNormalizedTagLine(String normalizedGameName, String normalizedTagLine);

    // 정규화된 이름 기준 여러 캐시 찾음
    // IgnoreCase -> 대소문자 구분 없이 조회
    List<RiotIdCacheEntity> findTop10ByNormalizedGameNameContaining(String normalizedGameName);

    // 자동완성용 - 소문자/대문자 구분 x 유사 검색 (LIKE 또는 IgnoreCase)
    List<RiotIdCacheEntity> findTop10ByGameNameIgnoreCaseContaining(String partialGameName);

    // 캐시에 같은 riot Id 가 있는지 확인하는 용도
    // 대소문자 구분 없이 존재 여부 확인
    boolean existsByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);

    Optional<RiotIdCacheEntity> findByPuuid(String puuid);

}


