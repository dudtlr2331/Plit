package com.plit.FO.party.entity;

import com.plit.FO.party.PositionEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "party_find_position")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyFindPositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_seq", nullable = false)
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PositionEnum position;
}