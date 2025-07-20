package com.plit.FO.qna.inquirychat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_room")
@Getter
@Setter
public class InquiryRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryRoomId;

    private Long userId;
    private Long adminId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}