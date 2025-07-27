package com.plit.FO.blacklist.repository;

import com.plit.FO.blacklist.entity.BlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntity, Integer> {
    List<BlacklistEntity> findAll();
    int countByReportedUserId(Integer reportedUserId);
    List<BlacklistEntity> findByReportedUserId(Integer reportedUserId);
    boolean existsByReporterIdAndReportedUserId(Integer reporterId, Integer reportedUserId);
    List<BlacklistEntity> findAllByOrderByReportedAtDesc();

    @Query("SELECT b.reportedUserId FROM BlacklistEntity b WHERE b.reporterId = :reporterId")
    List<Integer> findReportedUserIdsByReporterId(@Param("reporterId") Integer reporterId);

}