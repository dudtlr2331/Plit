package com.plit.FO.matchHistory.controller;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.entity.RiotIdCacheEntity;
import com.plit.FO.matchHistory.repository.RiotIdCacheRepository;
import com.plit.FO.matchHistory.service.MatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchHistoryController {

    private final MatchHistoryService matchHistoryService;
    private final RiotIdCacheRepository riotIdCacheRepository;

    @GetMapping
    public String getMatchPage(@RequestParam String gameName,
                               @RequestParam String tagLine,
                               Model model) {

        // puuid 가져오기 (자동완성에서 캐시되지 않았으면 Riot API 요청)
        String puuid = matchHistoryService.getPuuidOrRequest(gameName, tagLine);

        // puuid로 소환사 정보 가져오기
        SummonerDTO summoner = matchHistoryService.getSummonerByPuuid(puuid);
        if (summoner == null) {
            throw new IllegalArgumentException("잘못된 Riot ID입니다.");
        }

        // puuid로 매치 정보 조회
        List<MatchHistoryDTO> matchList = matchHistoryService.getMatchHistory(puuid);
        String tier = matchHistoryService.getTierByPuuid(puuid);
        MatchSummaryDTO summary = matchHistoryService.getMatchSummary(matchList);
        Map<String, RankDTO> rankMap = matchHistoryService.getRankInfoByPuuid(puuid);
        List<FavoriteChampionDTO> favoriteChampions = matchHistoryService.getFavoriteChampions(matchList);

        // 통계용 스타일 계산
        Map<String, String> championHeightStyles = summary.getChampionTotalGames().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            int total = summary.getTotalCount();
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));

        Map<String, String> positionHeightStyles = summary.getFavoritePositions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            int total = summary.getTotalCount();
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));

        Map<String, String> modeMap = Map.of(
                "CHERRY", "아레나",
                "CLASSIC", "소환사의 협곡",
                "ARAM", "칼바람 나락",
                "TUTORIAL", "튜토리얼"
        );

        // 모델에 데이터 넣기
        model.addAttribute("summoner", summoner);
        model.addAttribute("tier", tier);
        model.addAttribute("matchList", matchList);
        model.addAttribute("winCount", matchList.stream().filter(MatchHistoryDTO::isWin).count());
        model.addAttribute("totalCount", matchList.size());
        model.addAttribute("summary", summary);
        model.addAttribute("championHeightStyles", championHeightStyles);
        model.addAttribute("positionHeightStyles", positionHeightStyles);
        model.addAttribute("modeMap", modeMap);
        model.addAttribute("rankMap", rankMap);
        model.addAttribute("favoriteChampions", favoriteChampions);

        // 디버깅용 출력
        System.out.println("gameName = " + gameName);
        System.out.println("summoner: " + summoner);
        System.out.println("summoner puuid = " + summoner.getPuuid());
        System.out.println("profileIconUrl = " + summoner.getProfileIconUrl());

        return "fo/matchHistory/matchHistory"; // 템플릿 경로
    }

    @GetMapping("/detail")
    @ResponseBody
    public MatchDetailDTO getMatchDetail(@RequestParam String matchId,
                                         @RequestParam String puuid) {
        return matchHistoryService.getMatchDetail(matchId, puuid);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String keyword) {
        List<RiotIdCacheEntity> matches = riotIdCacheRepository
                .findTop10ByGameNameIgnoreCaseContaining(keyword.trim());

        // 중복 제거 및 "gameName#tagLine" 형태로 반환
        List<String> result = matches.stream()
                .map(e -> e.getGameName() + "#" + e.getTagLine())
                .distinct()
                .toList();

        return ResponseEntity.ok(result);
    }



    // 예비용
    @GetMapping("/history")
    public String getMatchHistoryByPuuid(@RequestParam String puuid, Model model) {

        SummonerDTO summoner = matchHistoryService.getSummonerByPuuid(puuid);
        if (summoner == null) {
            throw new IllegalArgumentException("존재하지 않는 puuid입니다.");
        }

        List<MatchHistoryDTO> matchList = matchHistoryService.getMatchHistory(puuid);
        String tier = matchHistoryService.getTierByPuuid(puuid);
        MatchSummaryDTO summary = matchHistoryService.getMatchSummary(matchList);
        Map<String, RankDTO> rankMap = matchHistoryService.getRankInfoByPuuid(puuid);
        List<FavoriteChampionDTO> favoriteChampions = matchHistoryService.getFavoriteChampions(matchList);

        Map<String, String> championHeightStyles = summary.getChampionTotalGames().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            int total = summary.getTotalCount();
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));

        Map<String, String> positionHeightStyles = summary.getFavoritePositions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            int total = summary.getTotalCount();
                            double height = total > 0 ? (value * 100.0 / total) : 0.0;
                            return "height:" + height + "%";
                        }
                ));

        Map<String, String> modeMap = Map.of(
                "CHERRY", "아레나",
                "CLASSIC", "소환사의 협곡",
                "ARAM", "칼바람 나락",
                "TUTORIAL", "튜토리얼"
        );

        model.addAttribute("summoner", summoner);
        model.addAttribute("tier", tier);
        model.addAttribute("matchList", matchList);
        model.addAttribute("winCount", matchList.stream().filter(MatchHistoryDTO::isWin).count());
        model.addAttribute("totalCount", matchList.size());
        model.addAttribute("summary", summary);
        model.addAttribute("championHeightStyles", championHeightStyles);
        model.addAttribute("positionHeightStyles", positionHeightStyles);
        model.addAttribute("modeMap", modeMap);
        model.addAttribute("rankMap", rankMap);
        model.addAttribute("favoriteChampions", favoriteChampions);

        System.out.println("puuid 요청으로 조회된 소환사: " + summoner.getGameName() + "#" + summoner.getTagLine());

        return "fo/matchHistory/matchHistory";
    }


}
