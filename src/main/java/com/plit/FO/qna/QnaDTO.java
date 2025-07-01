package com.plit.FO.qna;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class QnaDTO {
    private String title;
    private String content;
    private MultipartFile file;
    private String fileName;
    private String category;
}
