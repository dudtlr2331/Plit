package com.plit.FO.clan.entity;

import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.clan.enums.Position;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clan_id")
    private ClanEntity clan;

    // 신청자 ID (UserSeq)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Position position;

    private String intro;

    private LocalDateTime requestAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JoinStatus status;

    @Column(name = "tier")
    private String tier;
}