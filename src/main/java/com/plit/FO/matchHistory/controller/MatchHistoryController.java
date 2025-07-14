//package com.plit.FO.matchHistory.controller;
//
//import com.plit.FO.matchHistory.dto.*;
//import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
//import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
//import com.plit.FO.matchHistory.service.ImageService;
//import com.plit.FO.matchHistory.service.MatchDbService;
//import com.plit.FO.matchHistory.service.MatchHistoryService;
//import com.plit.FO.matchHistory.service.RiotApiService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Controller
//@RequestMapping("/match")
//@RequiredArgsConstructor
//public class MatchHistoryController {
//
//    private final ImageService imageService;
//    private final MatchHistoryService matchHistoryService;
//    private final RiotIdCacheRepository riotIdCacheRepository;
//    private final RiotApiService riotApiService;
//    private final MatchDbService matchDbService;
//
//    @GetMapping
//    public String getMatchPage(@RequestParam String gameName,
//                               @RequestParam String tagLine,
//                               Model model) {
//        // puuid 조회 (캐싱된 값 있으면 사용)
//        String puuid = matchHistoryService.getPuuidOrRequest(gameName, tagLine);
//
//        // 소환사 기본 정보 (캐시 기반 또는 최소 조회)
//        SummonerDTO summoner = matchHistoryService.getSummonerByPuuid(puuid);
//        if (summoner == null) {
//            model.addAttribute("message", "소환사 정보를 찾을 수 없습니다.");
//            return "fo/matchHistory/matchHistory";
//        }
//
//        summoner.setProfileIconUrl(imageService.getProfileIconUrl(summoner.getProfileIconId()));
//
//        // DB에서 전적 요약 불러오기
//        List<MatchSummaryDTO> matchList = matchHistoryService.getMatchSummariesFromDb(puuid);
//        if (matchList.isEmpty()) {
//            model.addAttribute("message", "저장된 전적이 없습니다. '전적 갱신' 버튼을 눌러주세요.");
//            model.addAttribute("summoner", summoner);
//            return "fo/matchHistory/matchHistory";
//        }
//
//        // 티어 + 티어 이미지
//        String tier = riotApiService.getTierByPuuid(puuid);
//        String tierImageUrl = imageService.getTierImageUrl(tier);
//
//        // 요약 통계
//        MatchSummaryDTO summary = matchHistoryService.getMatchSummary(matchList);
//
//        // 통계용 height style 계산 (챔피언 선호도, 포지션)
//        Map<String, String> championHeightStyles = summary.getChampionTotalGames().entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> {
//                            int value = e.getValue() != null ? e.getValue() : 0;
//                            int total = summary.getTotalCount();
//                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
//                            return "height:" + height + "%";
//                        }
//                ));
//
//        Map<String, String> positionHeightStyles = summary.getFavoritePositions().entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> {
//                            int value = e.getValue() != null ? e.getValue() : 0;
//                            int total = summary.getTotalCount();
//                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
//                            return "height:" + height + "%";
//                        }
//                ));
//
//        // 7. 모드 매핑
//        Map<String, String> modeMap = Map.of(
//                "CHERRY", "아레나",
//                "CLASSIC", "소환사의 협곡",
//                "ARAM", "칼바람 나락",
//                "TUTORIAL", "튜토리얼",
//                "AI", "AI 상대 대전",
//                "CHALLENGEGG", "격전"
//        );
//
//        // 8. model에 데이터 전달
//        model.addAttribute("summoner", summoner);
//        model.addAttribute("tier", tier);
//        model.addAttribute("tierImageUrl", tierImageUrl);
//        model.addAttribute("matchList", matchList);
//        model.addAttribute("winCount", matchList.stream().filter(MatchSummaryDTO::isWin).count());
//        model.addAttribute("totalCount", matchList.size());
//        model.addAttribute("summary", summary);
//        model.addAttribute("championHeightStyles", championHeightStyles);
//        model.addAttribute("positionHeightStyles", positionHeightStyles);
//        model.addAttribute("modeMap", modeMap);
//
//        return "fo/matchHistory/matchHistory";
//    }
//
//    @GetMapping("/favorite-champions/all")
//    public Map<String, List<FavoriteChampionDTO>> getAllFavoriteChampions(@RequestParam String puuid) {
//        Map<String, List<FavoriteChampionDTO>> result = new HashMap<>();
//
//
//        // overall : ui 에서 쓸 key 이름  /  all : 실제 모드명
//        result.put("overall", matchHistoryService.getFavoriteChampionsBySeason(puuid, "all"));
//        result.put("solo", matchHistoryService.getFavoriteChampionsBySeason(puuid, "solo"));
//        result.put("flex", matchHistoryService.getFavoriteChampionsBySeason(puuid, "flex"));
//
//        return result;
//    }
//
//    @GetMapping("/detail")
//    @ResponseBody
//    public MatchDetailDTO getMatchDetail(@RequestParam String matchId,
//                                         @RequestParam String puuid) {
//        return matchDbService.getMatchDetailFromRiot(matchId, puuid);
//    }
//
//    @GetMapping("/autocomplete")
//    public ResponseEntity<List<String>> autocomplete(@RequestParam String keyword) {
//        String normalized = keyword.trim().replaceAll("\\s+", "").toLowerCase();
//
//        List<RiotIdCacheEntity> matches = riotIdCacheRepository
//                .findTop10ByNormalizedGameNameContaining(normalized);
//
//        List<String> result = matches.stream()
//                .map(e -> e.getGameName() + "#" + e.getTagLine())
//                .distinct()
//                .toList();
//
//        return ResponseEntity.ok(result);
//    }
//
//
//    // http://localhost:8080/match/test-clan - (*) service 메서드 - test.html 참고
//    @GetMapping("/test-clan")
//    public String testClanMemberStats(Model model) {
//
//        // 테스트용 Riot ID
//        String gameName = "뭉청망청";
//        String tagLine = "kr1";
//
//        System.out.println("테스트: 멤버 = " + gameName + "#" + tagLine);
//
//        // Riot ID -> puuid
//        SummonerDTO summoner = riotApiService.getAccountByRiotId(gameName, tagLine);
//        if (summoner == null) {
//            throw new IllegalArgumentException("소환사 정보를 찾을 수 없습니다.");
//        }
//        String puuid = summoner.getPuuid();
//
//        // 티어
//        String tier = riotApiService.getTierByPuuid(puuid);
//        String tierImage = "/images/tier/" + tier.split(" ")[0].toUpperCase().replace(" ", "") + ".png";
//
//        // 전적 + 통계 요약
//        List<MatchHistoryDTO> matchList = matchDbService.getMatchHistoryFromRiot(puuid);
//        MatchSummaryDTO summary = matchHistoryService.getMatchSummary(matchList);
//
//        // 선호 챔피언
//        String mostChamp = summary.getSortedChampionList().isEmpty()
//                ? "데이터 없음"
//                : summary.getSortedChampionList().get(0).getKey();
//
//        // 승률, KDA 계산
//        int totalGames = summary.getTotalCount();
//        int wins = summary.getWinCount();
//        String winRate = (totalGames > 0) ? (Math.round(wins * 100.0 / totalGames) + "%") : "0%";
//        String kda = String.format("%.2f", summary.getKdaRatio());
//
//        // 모델 전달
//        model.addAttribute("summonerName", gameName + "#" + tagLine);
//        model.addAttribute("tier", tier);
//        model.addAttribute("tierImage", tierImage);
//        model.addAttribute("mostChampion", mostChamp);
//        model.addAttribute("winRate", winRate);
//        model.addAttribute("kda", kda);
//
//        return "fo/matchHistory/test";
//    }
//
//
//
//
//
//}
