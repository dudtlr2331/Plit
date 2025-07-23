package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.FavoriteChampionDTO;
import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
import com.plit.FO.matchHistory.dto.MatchSummaryWithListDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
import com.plit.FO.matchHistory.dto.db.UserMatchSummaryDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface MatchDbService {

    List<String> getMatchIdsByPuuid(String puuid);

    MatchDetailDTO getMatchDetailFromRiot(String matchId, String puuid);

    MatchDetailDTO getMatchDetailFromDb(String matchId, String puuid);

    // MatchList와 Summary, FavoriteChampions를 한 번에 가져오는 메서드
    MatchSummaryWithListDTO getSummaryAndList(String puuid);

    List<MatchHistoryDTO> getRecentMatchHistories(String puuid);

    String findPuuidInCache(String normalizedGameName, String normalizedTagLine);
    void saveRiotIdCache(String gameName, String tagLine, String normalizedGameName, String normalizedTagLine, String puuid);

    void saveMatchHistory(String puuid);

    void saveMatchSummaryAndPlayers(String gameName, String tagLine, String tier);

    boolean existsMatchByPuuid(String puuid);

    // 선호 챔피언 정보 저장
    void saveFavoriteChampions(String puuid, List<FavoriteChampionDTO> dtoList);
    // 전체 전적 요약 저장
    void saveOverallSummary(MatchOverallSummaryDTO dto);
    // summary( 각 매치 요약 ) / player ( 각 매치 플레이어들 ) 저장용 메서드
    void saveMatchHistory(MatchSummaryEntity summary, List<MatchPlayerEntity> players);
    // matchList -> 전적 통계 계산
    List<FavoriteChampionDTO> calculateFavoriteChampions(List<MatchHistoryDTO> matchList, String mode, String puuid);

    MatchOverallSummaryDTO calculateOverallSummary(List<MatchHistoryDTO> matchList, String puuid);

    Map<String, List<FavoriteChampionDTO>> getFavoriteChampionsAll(String puuid);

    // 존재하지 않은 경우 전체 최초 저장
    void fetchAndSaveAllIfNotExists(String puuid, String tagLine);
    // 기존 전적 최신화용
    void updateMatchHistory(String puuid);

    List<FavoriteChampionDTO> getFavoriteChampions(String puuid, String queueType);

    // 어리고싶다#kr1 저장 샘플
    void testSave(String gameName, String tagLine, String tier);

//    UserMatchSummaryDTO getUserMatchSummary(String puuid);
    void saveOnlyOverallSummary(String gameName, String tagLine, String tier);

    MatchOverallSummaryDTO getOverallSummary(String puuid);

    List<MatchHistoryDTO> fetchFavoriteChampionMatches(String gameName, String tagLine);

    void saveFavoriteChampionOnly(String gameName, String tagLine);

    void overwriteTier(String gameName, String tagLine, String tier);
}
