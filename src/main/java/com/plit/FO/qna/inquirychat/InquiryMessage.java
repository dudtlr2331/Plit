package com.plit.FO.qna.inquirychat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_message")
@Getter
@Setter
public class InquiryMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryMessageId;

    private Long inquiryRoomId;
    private Long senderId;

    @Lob
    private String content;

    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}