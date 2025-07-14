package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.MatchHistoryDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;

import java.util.List;

public interface MatchDbService {

    List<String> getMatchIdsByPuuid(String puuid);

    List<MatchHistoryDTO> getMatchHistoryFromRiot(String puuid);

    MatchDetailDTO getMatchDetailFromRiot(String matchId, String puuid);

    void saveMatchHistory(MatchSummaryEntity summary, List<MatchPlayerEntity> players);


    MatchDetailDTO getMatchDetailFromDB(String matchId);

    List<MatchHistoryDTO> getMatchSummaryFromDB(String puuid);

    void updateMatches(String puuid);
}
