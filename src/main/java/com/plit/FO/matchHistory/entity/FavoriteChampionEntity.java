package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.FavoriteChampionDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Entity
@Table(name = "favorite_champion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteChampionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String puuid;
    private String championName;

    private String queueType;  // overall, solo, flex

    @Column(name = "play_count")
    private int playCount;

    @Column(name = "win_count")
    private int winCount;
    private double winRate;

    @Column(name = "kda_ratio")
    private Double kdaRatio;

    @Column(name = "average_kills")
    private Double averageKills;

    @Column(name = "average_deaths")
    private Double averageDeaths;

    @Column(name = "average_assists")
    private Double averageAssists;

    @Column(name = "average_cs")
    private Double averageCs;

    @Column(name = "cs_per_min")
    private Double csPerMin;

    @Transient // DB에 저장되지 않음
    private String championImageUrl;

    public static FavoriteChampionEntity fromDTO(FavoriteChampionDTO dto) {
        FavoriteChampionEntity entity = new FavoriteChampionEntity();
        entity.setPuuid(dto.getPuuid());
        entity.setChampionName(dto.getChampionName());
        entity.setQueueType(dto.getQueueType());
        entity.setPlayCount(dto.getGameCount());
        entity.setWinCount(dto.getWinCount());
        entity.setWinRate(round(dto.getWinRate(),0));
        entity.setKdaRatio(round(dto.getKdaRatio(),2));
        entity.setAverageKills(round(dto.getKills(), 1));
        entity.setAverageDeaths(round(dto.getDeaths(), 1));
        entity.setAverageAssists(round(dto.getAssists(), 1));
        entity.setAverageCs(round(dto.getAverageCs(), 0));
        entity.setCsPerMin(round(dto.getCsPerMin(), 1));
        entity.setChampionImageUrl(dto.getChampionImageUrl());
        return entity;
    }
}

