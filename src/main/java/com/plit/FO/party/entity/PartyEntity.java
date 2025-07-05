package com.plit.FO.party.entity;

import com.plit.FO.party.enums.PartyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "party")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_seq")
    private Long partySeq;

    @Column(name = "party_name", nullable = false, length = 30)
    private String partyName;

    @Column(name = "party_type", nullable = false, length = 6)
    private String partyType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime partyCreateDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String  createdBy;

    @Column(name = "party_end_time", nullable = false)
    private LocalDateTime partyEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_status")
    private PartyStatus partyStatus;

    @Column(name = "party_headcount", nullable = false)
    private Integer partyHeadcount;

    @Column(name = "party_max", nullable = false)
    private Integer partyMax;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "main_position", nullable = false, length = 10)
    private String mainPosition;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyFindPositionEntity> partyFindPositions = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyMemberEntity> partyMembers = new ArrayList<>();
}
