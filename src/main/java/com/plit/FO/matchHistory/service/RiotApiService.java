package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.RankDTO;
import com.plit.FO.matchHistory.dto.SummonerSimpleDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;

import java.util.List;
import java.util.Map;

public interface RiotApiService {

    SummonerSimpleDTO getAccountByRiotId(String gameName, String tagLine);

    String getTierByPuuid(String puuid);

    Map<String, RankDTO> getRankInfoByPuuid(String puuid);

    List<String> getRecentMatchIds(String puuid, int count);

    RiotMatchInfoDTO getMatchInfo(String matchId);

}
