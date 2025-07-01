package com.plit.FO.party;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "party_find_position")
@Getter
@Setter
public class PartyFindPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_seq")
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PositionEnum position;
}