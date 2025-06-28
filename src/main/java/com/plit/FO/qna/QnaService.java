package com.plit.FO.qna;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class QnaService {

    private final QnaRepository qnaRepository;

    public QnaService(QnaRepository qnaRepository) {
        this.qnaRepository = qnaRepository;
    }

    // 질문 저장 메서드
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
            qna.setFileName(savedName); // DB 저장용
        }

        qnaRepository.save(qna);

        System.out.println("문의 등록됨 → " + qna.getTitle());
    }

    public List<QnaEntity> getMyQuestions(Long userId) {
        return qnaRepository.findByUserIdOrderByAskedAtDesc(userId);
    }

    public void deleteQna(Long id, Long userId) {
        qnaRepository.findById(id).ifPresent(qna -> {
            if (qna.getUserId().equals(userId)) {
                qnaRepository.delete(qna);
                System.out.println("문의 삭제됨: " + id);
            } else {
                System.out.println("삭제 권한 없음: " + userId);
            }
        });
    }


    //관리자

    // 관리자 전체 문의 내역 조회
    public List<QnaEntity> getAllQuestions() {
        return qnaRepository.findAll();
    }
    // 관리자용 - 특정 문의글 조회
    public QnaEntity findById(Long id) {
        return qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의글이 존재하지 않습니다."));
    }

    // 관리자용 - 답변 저장
    public void saveAnswer(Long id, String answer) {
        QnaEntity qna = findById(id);
        qna.setAnswer(answer);
        qna.setStatus("답변완료");
        qna.setAnsweredAt(LocalDateTime.now());
        qnaRepository.save(qna);
    }
}
