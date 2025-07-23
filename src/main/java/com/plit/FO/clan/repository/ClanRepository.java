package com.plit.FO.clan.repository;

import com.plit.FO.clan.entity.ClanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClanRepository extends JpaRepository<ClanEntity, Long> {

    List<ClanEntity> findByUseYnOrderByCreatedAtDesc(String useYn);

    Optional<ClanEntity> findByLeaderIdAndUseYn(Long leaderId, String useYn);

    boolean existsByNameAndUseYn(String name, String useYn);

    boolean existsByLeaderIdAndUseYn(Long leaderId, String useYn);

    @Query("SELECT c FROM ClanEntity c " +
            "WHERE (:tier IS NULL OR c.minTier = :tier) " +
            "AND ("
            + "(:keyword IS NULL) "
            + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.intro) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.minTier) LIKE LOWER(CONCAT('%', :keyword, '%'))"
            + ") " +
            "ORDER BY c.createdAt DESC")
    List<ClanEntity> searchByTierAndKeyword(@Param("tier") String tier,
                                            @Param("keyword") String keyword);

}

