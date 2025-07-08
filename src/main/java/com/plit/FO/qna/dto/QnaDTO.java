package com.plit.FO.qna.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class QnaDTO {

    private Long id;

    private String title;
    private String content;

    private MultipartFile file;   // 사용자 작성 시 첨부
    private String fileName;      // 저장된 실제 파일 이름
    private String category;      // 카테고리

    // 조회용
    private Long userId;
    private String userNickname;

    private String status;             // "대기중", "답변완료"
    private LocalDateTime askedAt;     // 작성일
}