package com.plit.FO.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntity, Integer> {
    // 추후 사용자별 신고 내역 조회용 메서드도 추가 가능
    List<BlacklistEntity> findAll();
    int countByReportedUserId(Integer reportedUserId);
    List<BlacklistEntity> findByReportedUserId(Integer reportedUserId);
    boolean existsByReporterIdAndReportedUserId(Integer reporterId, Integer reportedUserId);
    List<BlacklistEntity> findAllByOrderByReportedAtDesc();
}