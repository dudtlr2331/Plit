package com.plit.FO.qna;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaRepository extends JpaRepository<QnaEntity,Long> {
    List<QnaEntity> findByUserId(Long userId);
    List<QnaEntity> findByUserIdOrderByAskedAtDesc(Long userId);
}
