package com.plit.FO.qna;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaRepository extends JpaRepository<QnaEntity,Long> {
    List<QnaEntity> findByUserId(Long userId);
    List<QnaEntity> findByUserIdOrderByAskedAtDesc(Long userId);
    List<QnaEntity> findByUserIdAndDeleteYnOrderByAskedAtDesc(Long userId, String deleteYn);
    List<QnaEntity> findByDeleteYnOrderByAskedAtDesc(String deleteYn);

    @Modifying
    @Transactional
    @Query("UPDATE QnaEntity q SET q.deleteYn = 'Y' WHERE q.id = :id AND q.userId = :userId")
    void softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
