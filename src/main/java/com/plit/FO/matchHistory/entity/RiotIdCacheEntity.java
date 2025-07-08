package com.plit.FO.matchHistory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "riot_id_cache", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"gameName", "tagLine"}) // 제약조건
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiotIdCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // camelCase -> DB 에 snake_case 자동 매핑
    private String gameName; // game_name
    private String tagLine; // tag_line
    private String puuid;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}