package com.plit.FO.matchHistory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity // JPA 테이블 매핑
@Table(name = "image", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "type"})) // 테이블 이름, 유니크 제약
@Getter
@Setter
@NoArgsConstructor // 파라미터 없는 생성자
@AllArgsConstructor // 모든 필드 다 받는 생성자
@Builder // 빌더 패턴
@ToString
public class ImageEntity { // 이미지 경로 url 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @CreationTimestamp // 저장시 자동으로 현재 시각
    private LocalDateTime createdAt;

    // 자주 쓰는 필드 지정 생성자
    public ImageEntity(String name, String type, String imageUrl) {
        this.name = name;
        this.type = type;
        this.imageUrl = imageUrl;
    }

}
