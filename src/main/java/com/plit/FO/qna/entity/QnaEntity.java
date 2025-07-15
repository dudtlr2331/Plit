package com.plit.FO.qna.entity;

import com.plit.FO.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qna")
@Getter
@Setter
@NoArgsConstructor
public class QnaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status = "대기중";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "asked_at")
    private LocalDateTime askedAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "category")
    private String category;

    @Column(name = "delete_yn", columnDefinition = "VARCHAR(1) DEFAULT 'N'")
    private String deleteYn = "N";

    @Column(name = "admin_deleted", nullable = false)
    private boolean adminDeleted = false;

    public String getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Integer getUserSeq() {
        return user != null ? user.getUserSeq() : null;
    }

    public String getUserNickname() {
        return user != null ? user.getUserNickname() : null;
    }
}