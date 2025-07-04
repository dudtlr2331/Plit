package com.plit.FO.qna.service;

import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;

import java.util.List;

public interface QnaService {

    void saveQuestion(QnaDTO dto, Long userId);

    List<QnaEntity> getMyQuestions(Long userId);

    void deleteQna(Long id, Long userId);

    List<QnaEntity> getAllQuestions();

    QnaEntity findById(Long id);

    void saveAnswer(Long id, String answer);

    List<QnaEntity> getUnansweredQuestions();
}