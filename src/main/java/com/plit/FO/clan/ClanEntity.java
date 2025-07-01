package com.plit.FO.clan;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clan_id")
    private Long id;  // 클랜 ID

    @Column(name = "clan_name", nullable = false, length = 100)
    private String name;  // 클랜 이름

    @Column(name = "clan_intro", columnDefinition = "TEXT")
    private String intro;  // 소개글

    @Column(name = "kakao_link", length = 255)
    private String kakaoLink;  // 카카오톡 오픈채팅 링크

    @Column(name = "discord_link", length = 255)
    private String discordLink;  // 디스코드 초대 링크

    @Column(name = "tier", length = 30)
    private String tier;  // 클랜 자체 티어 수준 (선택사항)

    @Column(name = "min_tier", length = 30)
    private String minTier;  // 가입 가능한 최소 티어

    @Column(name = "clan_created_at", updatable = false)
    private LocalDateTime createdAt;  // 생성일

    @Column(name = "image_url", length = 255)
    private String imageUrl; // 클랜 대표 이미지 URL

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // 생성될 때 시간 자동 설정
    }
}