package com.plit.FO.qna.service;

import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;

import java.util.List;

public interface QnaService {

    // 사용자
    void saveQuestion(QnaDTO dto, Long userId);

    List<QnaEntity> getMyQuestions(Long userId);

    void deleteQna(Long id, Long userId);

    // 관리자
    List<QnaEntity> getAllQuestions();

    QnaEntity findById(Long id);

    void saveAnswer(Long id, String answer);

    List<QnaEntity> getUnansweredQuestions();

    // 관리자용 상태 필터
    List<QnaEntity> getQuestionsByType(String type);
}