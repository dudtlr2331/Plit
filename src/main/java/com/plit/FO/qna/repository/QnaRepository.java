package com.plit.FO.qna.repository;

import com.plit.FO.qna.entity.QnaEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaRepository extends JpaRepository<QnaEntity, Long> {

    // 사용자 문의 조회용
    List<QnaEntity> findByUserId(Long userId);
    List<QnaEntity> findByUserIdOrderByAskedAtDesc(Long userId);
    List<QnaEntity> findByUserIdAndDeleteYnOrderByAskedAtDesc(Long userId, String deleteYn);

    // 전체 문의 (삭제되지 않은 것만)
    List<QnaEntity> findByDeleteYnOrderByAskedAtDesc(String deleteYn);

    // 미답변 문의 (관리자 필터용)
    List<QnaEntity> findByAnswerIsNull();
    List<QnaEntity> findByAnswerIsNullAndDeleteYnOrderByAskedAtDesc(String deleteYn);

    // 답변완료 필터용
    List<QnaEntity> findByDeleteYnAndStatusOrderByAskedAtDesc(String deleteYn, String status);
    // 답변이 아직 작성되지 않은 "대기중" 상태만 필터
    List<QnaEntity> findByStatusAndDeleteYnOrderByAskedAtDesc(String status, String deleteYn);

    // 소프트 삭제
    @Modifying
    @Transactional
    @Query("UPDATE QnaEntity q SET q.deleteYn = 'Y' WHERE q.id = :id AND q.userId = :userId")
    void softDelete(@Param("id") Long id, @Param("userId") Long userId);

}