package com.plit.FO.clan;

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

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now(); // 기본 가입일 자동 설정
        this.status = "APPROVED"; // 기본 상태
    }
}