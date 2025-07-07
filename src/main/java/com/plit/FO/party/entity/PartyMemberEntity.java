package com.plit.FO.party.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"party_seq", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_seq", nullable = false)
    private PartyEntity party;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "join_time")
    private LocalDateTime joinTime = LocalDateTime.now();

    @Column(name = "role", nullable = false, length = 10)
    private String role = "MEMBER";
}
