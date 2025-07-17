package com.plit.FO.qna.service;

import com.plit.FO.qna.dto.QnaDTO;
import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.repository.QnaRepository;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class QnaServiceImpl implements QnaService {

    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    public QnaServiceImpl(QnaRepository qnaRepository, UserRepository userRepository) {
        this.qnaRepository = qnaRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void saveQuestion(QnaDTO dto, Long userId) {
        UserEntity user = userRepository.findById(userId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        QnaEntity qna = new QnaEntity();
        qna.setUser(user);
        qna.setTitle(dto.getTitle());
        qna.setContent(dto.getContent());
        qna.setCategory(dto.getCategory());
        qna.setStatus("대기중");
        qna.setAskedAt(LocalDateTime.now());

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                Path saveDir = Paths.get(uploadDir);
                Files.createDirectories(saveDir);

                String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
                String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;

                Path savePath = saveDir.resolve(uniqueFileName);
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                }

                qna.setFileName(uniqueFileName);

            } catch (IOException e) {
                throw new RuntimeException("파일 업로드 중 오류 발생", e);
            }
        }

        try {
            qnaRepository.save(qna);
        } catch (Exception e) {
            throw new RuntimeException("문의 저장 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public List<QnaEntity> getMyQuestions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            return userRepository.findById(userId.intValue())
                    .map(user -> qnaRepository.findByUserAndDeleteYnOrderByAskedAtDesc(user, "N"))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException("내 문의글 목록 조회 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteQna(Long id, Long userId) {
        QnaEntity qna = qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의글을 찾을 수 없습니다."));

        if (qna.getUser() == null || !qna.getUser().getUserSeq().equals(userId.intValue())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        qnaRepository.softDelete(id, userId);
    }

    @Override
    public List<QnaEntity> getAllQuestions() {
        List<QnaEntity> result = qnaRepository.findByDeleteYnAndAdminDeletedFalseOrderByAskedAtDesc("N");

        if (result.isEmpty()) {
            System.out.println("등록된 문의글이 없습니다.");
        }

        return result;
    }

    @Override
    public QnaEntity findById(Long id) {
        return qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의글이 존재하지 않습니다."));
    }

    @Override
    public void saveAnswer(Long id, String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("답변 내용이 비어 있을 수 없습니다.");
        }

        QnaEntity qna = findById(id);
        qna.setAnswer(answer);
        qna.setStatus("답변완료");
        qna.setAnsweredAt(LocalDateTime.now());
        qnaRepository.save(qna);
    }

    @Override
    public List<QnaEntity> getUnansweredQuestions() {
        return qnaRepository.findByAnswerIsNullAndDeleteYnAndAdminDeletedFalseOrderByAskedAtDesc("N");
    }

    @Override
    public List<QnaEntity> getQuestionsByType(String type) {
        String status = null;

        if ("UNANSWERED".equalsIgnoreCase(type)) {
            status = "대기중";
        } else if ("ANSWERED".equalsIgnoreCase(type)) {
            status = "답변완료";
        }

        if (status != null) {
            return qnaRepository.findByDeleteYnAndStatusAndAdminDeletedFalseOrderByAskedAtDesc("N", status);
        } else {
            return qnaRepository.findByDeleteYnAndAdminDeletedFalseOrderByAskedAtDesc("N");
        }
    }

    @Override
    public QnaDTO toDTO(QnaEntity entity) {
        QnaDTO dto = new QnaDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setFileName(entity.getFileName());
        dto.setAskedAt(entity.getAskedAt());
        dto.setStatus(entity.getStatus());
        dto.setCategory(entity.getCategory());

        if (entity.getUser() != null) {
            dto.setUserId((long) entity.getUser().getUserSeq());
            dto.setUserNickname(entity.getUser().getUserNickname());
        } else {
            dto.setUserNickname("(탈퇴회원)");
        }

        return dto;
    }

    @Override
    public void deleteById(Long id) {
        try {
            QnaEntity qna = findById(id);
            qna.setAdminDeleted(true);
            qnaRepository.save(qna);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("관리자 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public List<QnaEntity> getDeletedQuestions() {
        return qnaRepository.findByDeleteYnOrAdminDeletedTrueOrderByAskedAtDesc("Y");
    }

    @Override
    public void hardDelete(Long id) {
        try {
            qnaRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("삭제할 문의글이 존재하지 않습니다.", e);
        }
    }
}