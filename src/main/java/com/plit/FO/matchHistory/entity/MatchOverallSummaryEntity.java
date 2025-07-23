package com.plit.FO.matchHistory.entity;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
import com.plit.FO.matchHistory.dto.db.UserMatchSummaryDTO;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Entity
@Table(name = "match_overall_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchOverallSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String puuid;

    @Column(length = 50)
    private String gameName;

    @Column(length = 20)
    private String tagLine;

    private String tier;

    private Integer totalMatches;
    private Integer totalWins;

    private Double winRate;

    private Double averageKills;
    private Double averageDeaths;
    private Double averageAssists;
    private Double averageKda;
    private Double averageCs;

    @Column(length = 20)
    private String preferredPosition;

    @Column(name = "position_counts", columnDefinition = "TEXT")
    private String positionCounts;

    @Column(columnDefinition = "TEXT")
    private String preferredChampions;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static MatchOverallSummaryEntity fromDTO(MatchOverallSummaryDTO dto) {
        return MatchOverallSummaryEntity.builder()
                .id(dto.getId())  // 있을 수도 있음
                .puuid(dto.getPuuid())
                .gameName(dto.getGameName())
                .tagLine(dto.getTagLine())
                .tier(dto.getTier())
                .totalMatches(dto.getTotalMatches())
                .totalWins(dto.getTotalWins())
                .winRate(round(dto.getWinRate(),0))
                .averageKills(dto.getAverageKills())
                .averageDeaths(dto.getAverageDeaths())
                .averageAssists(dto.getAverageAssists())
                .averageKda(round(dto.getAverageKda(), 2))
                .averageCs(dto.getAverageCs())
                .preferredPosition(dto.getPreferredPosition())
                .positionCounts(new Gson().toJson(dto.getPositionCounts()))
                .preferredChampions(String.join(",", dto.getPreferredChampions()))
                .build();
    }

    public MatchOverallSummaryDTO toDTO() {

        List<String> championList = Arrays.asList(this.preferredChampions.split(","));

        Map<String, Long> positionMap = new Gson().fromJson(
                this.positionCounts,
                new TypeToken<Map<String, Long>>() {}.getType()
        );

        return MatchOverallSummaryDTO.builder()
                .id(this.id)
                .puuid(this.puuid)
                .gameName(this.gameName)
                .tagLine(this.tagLine)
                .tier(this.tier)
                .totalMatches(this.totalMatches)
                .totalWins(this.totalWins)
                .winRate(round(this.winRate,0))
                .averageKills(this.averageKills)
                .averageDeaths(this.averageDeaths)
                .averageAssists(this.averageAssists)
                .averageKda(round(this.averageKda, 2))
                .averageCs(this.averageCs)
                .preferredPosition(this.preferredPosition)
                .favoritePositions(Map.of(
                        "TOP", 0.0,
                        "JUNGLE", 0.0,
                        "MIDDLE", 0.0,
                        "BOTTOM", 0.0,
                        "UTILITY", 0.0
                ))
                .sortedPositionList(List.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY"))
                .positionCounts(positionMap)
                .preferredChampions(championList)
                .createdAt(this.createdAt)
                .build();
    }

}
