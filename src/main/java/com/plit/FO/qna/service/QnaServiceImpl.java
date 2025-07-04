package com.plit.FO.qna.service;

import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.repository.QnaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class QnaServiceImpl implements QnaService {

    private final QnaRepository qnaRepository;

    public QnaServiceImpl(QnaRepository qnaRepository) {
        this.qnaRepository = qnaRepository;
    }

    @Override
    public void saveQuestion(QnaDTO dto, Long userId) {
        QnaEntity qna = new QnaEntity();
        qna.setUserId(userId);
        qna.setTitle(dto.getTitle());
        qna.setContent(dto.getContent());
        qna.setStatus("대기중");
        qna.setAskedAt(LocalDateTime.now());

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            String originalFilename = dto.getFile().getOriginalFilename();
            String savedName = UUID.randomUUID() + "_" + originalFilename;
            qna.setFileName(savedName);
        }

        qnaRepository.save(qna);
        System.out.println("문의 등록됨 → " + qna.getTitle());
    }

    @Override
    public List<QnaEntity> getMyQuestions(Long userId) {
        return qnaRepository.findByUserIdAndDeleteYnOrderByAskedAtDesc(userId, "N");
    }

    @Override
    public void deleteQna(Long id, Long userId) {
        qnaRepository.findById(id).ifPresent(qna -> {
            if (qna.getUserId().equals(userId)) {
                qnaRepository.softDelete(id, userId);
                System.out.println("문의 삭제됨: " + id);
            } else {
                System.out.println("삭제 권한 없음: " + userId);
            }
        });
    }

    @Override
    public List<QnaEntity> getAllQuestions() {
        return qnaRepository.findByDeleteYnOrderByAskedAtDesc("N");
    }

    @Override
    public QnaEntity findById(Long id) {
        return qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의글이 존재하지 않습니다."));
    }

    @Override
    public void saveAnswer(Long id, String answer) {
        QnaEntity qna = findById(id);
        qna.setAnswer(answer);
        qna.setStatus("답변완료");
        qna.setAnsweredAt(LocalDateTime.now());
        qnaRepository.save(qna);
    }
}