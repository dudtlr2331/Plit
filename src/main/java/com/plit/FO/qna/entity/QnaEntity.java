package com.plit.FO.qna.entity;

import com.plit.FO.user.UserEntity;
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

    // user_id를 외래키로 사용 (읽기 전용)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 실제 UserEntity와 연관관계 (읽기 전용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", insertable = false, updatable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private UserEntity user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status = "미처리";

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

    @Column(name = "delete_yn", columnDefinition = "VARCHAR(1) DEFAULT 'N'")
    private String deleteYn = "N";
}