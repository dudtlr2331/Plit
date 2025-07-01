package com.plit.FO.clan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClanRepository extends JpaRepository<ClanEntity, Long> {

    List<ClanEntity> findAllByOrderByCreatedAtDesc();

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
