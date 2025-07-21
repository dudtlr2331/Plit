//package com.plit.FO.matchHistory.controller;
//
//import com.plit.FO.matchHistory.dto.SummonerSimpleDTO;
//import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
//import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
//import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
//import com.plit.FO.matchHistory.service.MatchDbService;
//import com.plit.FO.matchHistory.service.MatchHistoryService;
//import com.plit.FO.matchHistory.service.RiotApiService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@RequestMapping("/match")
//@Controller
//@RequiredArgsConstructor
//public class TestController {
//
//    private final MatchHistoryService matchHistoryService;
//    private final RiotApiService riotApiService;
//    private final MatchDbService matchDbService;
//

//
//    @GetMapping("/test")
//    public String testImage() {
//        return "/fo/matchHistory/test";
//    }
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
//        SummonerSimpleDTO summoner = matchHistoryService.getAccountByRiotId(gameName, tagLine);
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
//    @GetMapping("/test2")
//    public String showTest2(Model model) {
//
//        MatchHistoryDTO match = MatchHistoryDTO.builder()
//                .matchId("KR_123456789")
//                .championName("Ahri")
//                .kills(10)
//                .deaths(2)
//                .assists(8)
//                .tier("Gold IV")
//                .gameMode("솔로 랭크")
//                .gameEndTimestamp(LocalDateTime.now())
//                .build();
//
//        model.addAttribute("match", match);
//
//        return "fo/matchHistory/test2";
//    }
//
//
//}
