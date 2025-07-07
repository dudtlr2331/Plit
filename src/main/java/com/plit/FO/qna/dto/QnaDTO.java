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
    private MultipartFile file;
    private String fileName;
    private String category;

    // 관리자용 조회 필드
    private Long userId; // Long으로 수정
    private String userNickname;
    private String status;
    private LocalDateTime askedAt;
}