package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;

import java.util.List;

public interface MatchHistoryService {

    String getPuuidOrRequest(String gameName, String tagLine);

    List<MatchHistoryDTO> getMatchHistory(String puuid);

    MatchDetailDTO getMatchDetail(String matchId);

    List<FavoriteChampionDTO> getFavoriteChampionsBySeason(String puuid, String season);

    MatchSummaryDTO getMatchSummary(List<MatchHistoryDTO> matchList);

    void saveMatchHistory(String puuid);
}
