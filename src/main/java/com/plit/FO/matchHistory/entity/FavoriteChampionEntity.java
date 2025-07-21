package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.FavoriteChampionDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Transient // DB에 저장되지 않음
    private String championImageUrl;

    public static FavoriteChampionEntity fromDTO(FavoriteChampionDTO dto) {
        FavoriteChampionEntity entity = new FavoriteChampionEntity();
        entity.setPuuid(dto.getPuuid());
        entity.setChampionName(dto.getChampionName());
        entity.setQueueType(dto.getQueueType());
        entity.setPlayCount(dto.getGameCount());
        entity.setWinCount(dto.getWinCount());
        entity.setWinRate(dto.getWinRate());
        entity.setKdaRatio(dto.getKdaRatio());
        entity.setChampionImageUrl(dto.getChampionImageUrl());
        return entity;
    }
}

