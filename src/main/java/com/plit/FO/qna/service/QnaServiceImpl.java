package com.plit.FO.qna.service;

import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.repository.QnaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class QnaServiceImpl implements QnaService {

    private final QnaRepository qnaRepository;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

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

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String savedName = UUID.randomUUID() + "_" + originalFilename;

            try {
                Path savePath = Paths.get(uploadDir).resolve(savedName);
                file.transferTo(savePath.toFile());
                qna.setFileName(savedName);
            } catch (IOException e) {
                throw new RuntimeException("파일 저장 실패", e);
            }
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

    @Override
    public List<QnaEntity> getUnansweredQuestions() {
        return qnaRepository.findByAnswerIsNullAndDeleteYnOrderByAskedAtDesc("N");
    }
}