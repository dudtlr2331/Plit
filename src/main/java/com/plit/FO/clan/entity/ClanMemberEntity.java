package com.plit.FO.clan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clan_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClanMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clan_member_id")
    private Long id;

    @Column(name = "clan_id", nullable = false)
    private Long clanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "status", length = 20)
    private String status; // 예: APPROVED, PENDING, REJECTED

    @Column(name = "main_position", length = 20)
    private String mainPosition;

    @Column(name = "role", length = 20, nullable = false)
    private String role; // 예: LEADER, MEMBER

    @Column(name = "tier", length = 20)
    private String tier; // 예: Gold, Silver 등

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro; // 자기소개 글

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now(); // 기본 가입일 자동 설정
        }
        if (this.status == null) {
            this.status = "APPROVED"; // 기본 상태
        }
        if (this.role == null) {
            this.role = "MEMBER"; // 기본 역할
        }
    }
}