package com.plit.FO.clan.entity;

import com.plit.FO.clan.enums.Position;
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
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private Position position;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "tier", length = 20)
    private String tier;

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "APPROVED";
        }
        if (this.role == null) {
            this.role = "MEMBER";
        }
        if (this.position == null) {
            this.position = Position.ALL;
        }
    }
}