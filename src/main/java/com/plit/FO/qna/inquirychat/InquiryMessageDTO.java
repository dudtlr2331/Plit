package com.plit.FO.qna.inquirychat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InquiryMessageDTO {
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;
}