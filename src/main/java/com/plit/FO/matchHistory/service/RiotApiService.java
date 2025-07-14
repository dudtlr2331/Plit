package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.RankDTO;
import com.plit.FO.matchHistory.dto.SummonerDTO;

import java.util.Map;

public interface RiotApiService {

    SummonerDTO getAccountByRiotId(String gameName, String tagLine);

    String getTierByPuuid(String puuid);

    Map<String, RankDTO> getRankInfoByPuuid(String puuid);

}
