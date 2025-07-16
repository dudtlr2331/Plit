package com.plit.FO.matchHistory.controller;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.MatchDbService;
import com.plit.FO.matchHistory.service.MatchHistoryService;
import com.plit.FO.matchHistory.service.RiotApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchHistoryController {

    private final ImageService imageService;
    private final MatchHistoryService matchHistoryService;
    private final RiotIdCacheRepository riotIdCacheRepository;
    private final RiotApiService riotApiService;
    private final MatchDbService matchDbService;

    @GetMapping
    public String getMatchPage(@RequestParam String gameName,
                               @RequestParam String tagLine,
                               Model model) {

        // 소환사 기본 정보
        SummonerSimpleDTO summoner = riotApiService.getAccountByRiotId(gameName, tagLine);
        model.addAttribute("summoner", summoner);
        model.addAttribute("gameName", gameName);
        model.addAttribute("tagLine", tagLine);

        // PUUID 조회
        String puuid = matchHistoryService.getPuuidOrRequest(gameName, tagLine);
        String message = null;
        if (puuid == null) {
            message = "해당 소환사의 정보를 찾을 수 없습니다.";
        }

        // 랭크 정보
        Map<String, RankDTO> rankMap = (puuid != null) ? riotApiService.getRankInfoByPuuid(puuid) : new HashMap<>();
        if (rankMap == null) rankMap = new HashMap<>();
        model.addAttribute("rankMap", rankMap);

        RankDTO rankDTO = rankMap.get("RANKED_SOLO_5x5");
        String profileIconUrl = null;
        String tierImageUrl = null;
        String tier = null;
        int summonerLevel = 0;

        if (rankDTO != null) {
            profileIconUrl = imageService.getImageUrl(String.valueOf(rankDTO.getProfileIconId()), "profile");
            tierImageUrl = imageService.getImageUrl(rankDTO.getTier() + ".png", "tier");
            tier = rankDTO.getTier();
            summonerLevel = rankDTO.getSummonerLevel();
        } else {
            message = (message == null) ? "랭크 정보를 불러올 수 없습니다." : message;
        }

        model.addAttribute("profileIconUrl", profileIconUrl);
        model.addAttribute("tierImageUrl", tierImageUrl);
        model.addAttribute("summonerLevel", summonerLevel);
        model.addAttribute("tier", tier);

        // 전적 요약
        List<MatchHistoryDTO> matchList = (puuid != null) ? matchDbService.getRecentMatchHistories(puuid) : new ArrayList<>();
        if (matchList == null || matchList.isEmpty()) {
            message = (message == null) ? "저장된 전적이 없습니다. '전적 갱신' 버튼을 눌러주세요." : message;
        }
        model.addAttribute("matchList", matchList);

        // 요약 통계
        MatchSummaryDTO summary = matchHistoryService.getMatchSummary(matchList);
        if (summary == null) summary = new MatchSummaryDTO();
        model.addAttribute("summary", summary);
        model.addAttribute("winCount", summary.getWinCount());
        model.addAttribute("totalCount", summary.getTotalCount());

        // 스타일 계산
        int total = summary.getTotalCount();
        Map<String, String> championHeightStyles = summary.getChampionTotalGames().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));
        Map<String, String> positionHeightStyles = summary.getFavoritePositions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));
        model.addAttribute("championHeightStyles", championHeightStyles);
        model.addAttribute("positionHeightStyles", positionHeightStyles);

        // 모드 매핑
        Map<String, String> modeMap = Map.of(
                "CHERRY", "아레나",
                "CLASSIC", "소환사의 협곡",
                "ARAM", "칼바람 나락",
                "TUTORIAL", "튜토리얼",
                "AI", "AI 상대 대전",
                "CHALLENGEGG", "격전"
        );
        model.addAttribute("modeMap", modeMap);

        // 메시지 최종 추가
        if (message != null) {
            model.addAttribute("message", message);
        }

        return "fo/matchHistory/matchHistory";
    }


    // 초기화 버튼 누르면 호출
    @GetMapping("/init") // 일단 get 방식으로..
    @ResponseBody
    public ResponseEntity<String> initMatchHistory(@RequestParam String puuid) {
        try {
            matchHistoryService.saveMatchHistory(puuid);  // Riot API -> DB 저장
            return ResponseEntity.ok("전적 초기화 완료!");
        } catch (Exception e) {
            log.error("전적 초기화 실패", e);
            return ResponseEntity.status(500).body("전적 초기화 실패: " + e.getMessage());
        }
    }

    // 전적갱신 버튼 누르면 호출
    @GetMapping("/update")
    @ResponseBody
    public ResponseEntity<String> updateMatchHistory(@RequestParam String puuid) {
        try {
            matchDbService.updateMatchHistory(puuid); // 아래에 만들어야 할 메서드
            return ResponseEntity.ok("전적 갱신이 완료되었습니다.");
        } catch (Exception e) {
            log.error("전적 갱신 중 오류 발생", e);
            return ResponseEntity.status(500).body("전적 갱신 실패: " + e.getMessage());
        }
    }



    @GetMapping("/favorite-champions/all")
    public Map<String, List<FavoriteChampionDTO>> getAllFavoriteChampions(@RequestParam String puuid) {
        Map<String, List<FavoriteChampionDTO>> result = new HashMap<>();


        // overall : ui 에서 쓸 key 이름  /  all : 실제 모드명
        result.put("overall", matchHistoryService.getFavoriteChampionsBySeason(puuid, "all"));
        result.put("solo", matchHistoryService.getFavoriteChampionsBySeason(puuid, "solo"));
        result.put("flex", matchHistoryService.getFavoriteChampionsBySeason(puuid, "flex"));

        return result;
    }

    // match 상세 페이지 요청
    @GetMapping("/detail")
    @ResponseBody
    public MatchDetailDTO getMatchDetail(@RequestParam String matchId,
                                         @RequestParam String puuid) {
        return matchDbService.getMatchDetailFromRiot(matchId, puuid);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String keyword) {
        String normalized = keyword.trim().replaceAll("\\s+", "").toLowerCase();

        List<RiotIdCacheEntity> matches = riotIdCacheRepository
                .findTop10ByNormalizedGameNameContaining(normalized);

        List<String> result = matches.stream()
                .map(e -> e.getGameName() + "#" + e.getTagLine())
                .distinct()
                .toList();

        return ResponseEntity.ok(result);
    }

}
