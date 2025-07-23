package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.FavoriteChampionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteChampionRepository extends JpaRepository<FavoriteChampionEntity, Long> {
    List<FavoriteChampionEntity> findByPuuid(String puuid);
    Optional<FavoriteChampionEntity> findByPuuidAndChampionName(String puuid, String championName);
    List<FavoriteChampionEntity> findByPuuidAndQueueType(String puuid, String queueType);
    void deleteByPuuid(String puuid);
    void deleteByPuuidAndQueueType(String puuid, String queueType);
}