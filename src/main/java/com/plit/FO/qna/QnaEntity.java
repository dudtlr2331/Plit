package com.plit.FO.qna;

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

    @Column(name = "user_id")
    private Long userId;

    private String title;
    private String content;
    private String answer;
    private String status;

    @Column(name = "asked_at")
    private LocalDateTime askedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

}
