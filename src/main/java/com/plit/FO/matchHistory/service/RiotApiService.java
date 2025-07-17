package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.RankDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.riot.RiotAccountResponse;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotSummonerResponse;

import java.util.List;
import java.util.Map;

public interface RiotApiService {

    String getTierByPuuid(String puuid);

    Map<String, RankDTO> getRankInfoByPuuid(String puuid);

    List<String> getRecentMatchIds(String puuid, int count);

    RiotMatchInfoDTO getMatchInfo(String matchId);

    MatchDetailDTO getMatchDetailFromRiot(String matchId, String puuid);

    RiotAccountResponse getAccountByRiotId(String gameName, String tagLine);
    RiotSummonerResponse getSummonerByPuuid(String puuid);

    String requestPuuidFromRiot(String gameName, String tagLine);
}
