package com.plit.FO.party.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "join_time", nullable = false, updatable = false)
    private LocalDateTime joinTime;

    @Column(name = "role", nullable = false, length = 10)
    private String role = "MEMBER";

    @Column(name = "message")
    private String message;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";  // PENDING, ACCEPTED, REJECTED

    @Column(name = "position", nullable = false)
    private String position;
}
