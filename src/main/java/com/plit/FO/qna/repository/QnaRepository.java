package com.plit.FO.qna.repository;

import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.user.entity.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaRepository extends JpaRepository<QnaEntity, Long> {

    // 사용자 문의 조회
    List<QnaEntity> findByUser(UserEntity user);

    List<QnaEntity> findByUserOrderByAskedAtDesc(UserEntity user);

    List<QnaEntity> findByUserAndDeleteYnOrderByAskedAtDesc(UserEntity user, String deleteYn);

    // 관리자용 문의 조회
    List<QnaEntity> findByDeleteYnAndAdminDeletedFalseOrderByAskedAtDesc(String deleteYn);

    List<QnaEntity> findByDeleteYnAndAdminDeletedFalseAndStatusOrderByAskedAtDesc(String deleteYn, String status);

    List<QnaEntity> findByAnswerIsNullAndDeleteYnAndAdminDeletedFalseOrderByAskedAtDesc(String deleteYn);

    List<QnaEntity> findByDeleteYnOrAdminDeletedTrueOrderByAskedAtDesc(String deleteYn);

    List<QnaEntity> findByDeleteYnAndStatusAndAdminDeletedFalseOrderByAskedAtDesc(String deleteYn, String status);

    // 소프트 삭제
    @Modifying
    @Transactional
    @Query("UPDATE QnaEntity q SET q.deleteYn = 'Y' WHERE q.id = :id AND q.user.userSeq = :userSeq")
    void softDelete(@Param("id") Long id, @Param("userSeq") Long userSeq);

    // 관리자 삭제
    @Modifying
    @Transactional
    @Query("UPDATE QnaEntity q SET q.adminDeleted = true WHERE q.id = :id")
    void softDeleteByAdmin(@Param("id") Long id);
}