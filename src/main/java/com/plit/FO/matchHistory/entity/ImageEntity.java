package com.plit.FO.matchHistory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ImageEntity {

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

    public ImageEntity(String name, String type, String imageUrl) {
        this.name = name;
        this.type = type;
        this.imageUrl = imageUrl;
    }

}
