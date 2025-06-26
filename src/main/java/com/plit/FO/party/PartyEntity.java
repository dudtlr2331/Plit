package com.plit.FO.party;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    @Column(name = "party_seq", nullable = false)
    private Long partySeq; // 번호

    @Column(name = "party_name", nullable = false, length = 30)
    private String partyName; // 파티 이름

    @Column(name = "party_type", nullable = false, length = 6)
    private String partyType; // 타입

    @Column(name = "party_create_date", nullable = false)
    private LocalDateTime partyCreateDate; // 생성일자

    @Column(name = "party_end_time", nullable = false)
    private LocalDateTime partyEndTime; // 파티 종료 일자

    @Column(name = "party_status", nullable = false, length = 7)
    private String partyStatus;  // 파티 상태 (WAITING, FULL, CLOSED 등)

    @Column(name = "party_headcount", nullable = false)
    private Integer partyHeadcount; // 파티 인원 수

    @Column(name = "party_max", nullable = false)
    private Integer partyMax; // 최대 모집 인원 수
}
