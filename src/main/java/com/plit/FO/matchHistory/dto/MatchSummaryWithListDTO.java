package com.plit.FO.matchHistory.dto;

import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSummaryWithListDTO {
    private MatchSummaryDTO summary;
    private List<MatchHistoryDTO> matchList;
    private List<FavoriteChampionDTO> favoriteChampions;
}
