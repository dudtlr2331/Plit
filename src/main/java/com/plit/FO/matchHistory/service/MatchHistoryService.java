package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface MatchHistoryService {

    SummonerSimpleDTO getAccountByRiotId(String gameName, String tagLine);

    String getPuuidOrRequest(String gameName, String tagLine);

    List<MatchHistoryDTO> getMatchHistory(String puuid);

    List<FavoriteChampionDTO> getFavoriteChampionsBySeason(String puuid, String season);

    MatchSummaryDTO getMatchSummary(List<MatchHistoryDTO> matchList);

    void saveMatchHistory(String puuid);

    Map<String, List<FavoriteChampionDTO>> getFavoriteChampionsAll(String puuid);

    MatchSummaryWithListDTO getSummaryAndListFromApi(String puuid);

}

