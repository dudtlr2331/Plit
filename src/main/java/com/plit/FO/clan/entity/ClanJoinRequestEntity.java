package com.plit.FO.clan.entity;

import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.party.enums.PositionEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clan_join_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClanJoinRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 클랜에 신청했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id")
    private ClanEntity clan;

    // 신청자 ID (UserSeq)
    private Long userId;

    // 주 포지션
    @Enumerated(EnumType.STRING)
    private PositionEnum mainPosition;

    // 자기소개
    private String intro;

    // 신청 시간
    private LocalDateTime requestAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JoinStatus status;

    @Column(name = "tier")
    private String tier;
}