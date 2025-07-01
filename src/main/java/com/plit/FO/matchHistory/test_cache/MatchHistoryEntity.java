package com.plit.FO.matchHistory.test_cache;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "riot_id_cache", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"gameName", "tagLine"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gameName;

    private String tagLine;

    private String puuid;

    private LocalDateTime createdAt = LocalDateTime.now();
}