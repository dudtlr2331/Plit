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

        // PUUID 조회
        String puuid = matchHistoryService.getPuuidOrRequest(gameName, tagLine);
        String message = null;
        if (puuid == null) {
            model.addAttribute("message", "해당 소환사의 정보를 찾을 수 없습니다.");
            return "fo/matchHistory/matchHistory";
        }

        // 소환사 기본 정보
        SummonerSimpleDTO summoner = matchHistoryService.getAccountByRiotId(gameName, tagLine);
        model.addAttribute("summoner", summoner);
        model.addAttribute("gameName", gameName);
        model.addAttribute("tagLine", tagLine);

        // 랭크 정보
        Map<String, RankDTO> rankMap = riotApiService.getRankInfoByPuuid(puuid);
        RankDTO soloRank = (rankMap != null) ? rankMap.get("RANKED_SOLO_5x5") : null;
        model.addAttribute("rankMap", rankMap);

        if (soloRank != null) {
            model.addAttribute("tier", soloRank.getTier());
            model.addAttribute("tierImageUrl", imageService.getImageUrl(soloRank.getTier() + ".png", "tier"));
            model.addAttribute("profileIconUrl", imageService.getImageUrl(String.valueOf(soloRank.getProfileIconId()), "profile"));
            model.addAttribute("summonerLevel", soloRank.getSummonerLevel());
        }


        // 선호 챔피언
        MatchSummaryWithListDTO dto = matchHistoryService.getSummaryAndListFromApi(puuid);

        model.addAttribute("summary", dto.getSummary());
        model.addAttribute("matchList", dto.getMatchList());
        model.addAttribute("favoriteChampions", dto.getFavoriteChampions());

        model.addAttribute("winCount", dto.getSummary().getWinCount());
        model.addAttribute("totalCount", dto.getSummary().getTotalCount());

        Map<String, List<FavoriteChampionDTO>> favoriteChampionsMap =
                matchHistoryService.getFavoriteChampionsAll(puuid);

        // 탭별 챔피언 리스트
        model.addAttribute("favoriteChampionsByMode", favoriteChampionsMap);

        model.addAttribute("overallChampions", favoriteChampionsMap.get("overall"));
        model.addAttribute("soloChampions", favoriteChampionsMap.get("solo"));
        model.addAttribute("flexChampions", favoriteChampionsMap.get("flex"));

        System.out.println("overall size: " + favoriteChampionsMap.get("overall").size());
        System.out.println("solo size: " + favoriteChampionsMap.get("solo").size());
        System.out.println("flex size: " + favoriteChampionsMap.get("flex").size());
        System.out.println("같은 객체? " + (favoriteChampionsMap.get("overall") == favoriteChampionsMap.get("solo")));


        // 스타일 계산
        int total = dto.getSummary().getTotalCount();
        Map<String, String> championHeightStyles = dto.getSummary().getChampionTotalGames().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));
        Map<String, String> positionHeightStyles = dto.getSummary().getFavoritePositions().entrySet().stream()
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

    // match 상세 페이지 요청
    @GetMapping("/detail")
    @ResponseBody
    public MatchDetailDTO getMatchDetail(@RequestParam String matchId,
                                         @RequestParam String puuid) {
        return riotApiService.getMatchDetailFromRiot(matchId, puuid);
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
