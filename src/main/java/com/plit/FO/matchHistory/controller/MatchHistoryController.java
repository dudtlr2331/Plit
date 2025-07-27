package com.plit.FO.matchHistory.controller;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
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

import java.util.*;
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

        // PUUID 조회( DB 에 있으면 가져오고 없으면 rot api 에서 받아오기 )
        String puuid = matchHistoryService.getPuuidOrRequest(gameName, tagLine);

        if (puuid == null) {
            model.addAttribute("message", "소환사 정보를 찾을 수 없습니다.");
            return "fo/matchHistory/matchHistory";
        }

        // 매치 기록도 DB 에 있는지
        boolean hasMatch = matchDbService.existsMatchByPuuid(puuid);

        // 초기화 필요한 경우 -> /match/init 호출
        if (!hasMatch) {
            model.addAttribute("initRequired", true);
            model.addAttribute("puuid", puuid);
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

        // 전적 요약 ( DB 기반 )
        MatchSummaryWithListDTO dto = matchDbService.getSummaryAndList(puuid);

        model.addAttribute("summary", dto.getSummary());
        model.addAttribute("matchList", dto.getMatchList());
        model.addAttribute("favoriteChampions", dto.getFavoriteChampions());

        model.addAttribute("winCount", dto.getSummary().getWinCount());
        model.addAttribute("totalCount", dto.getSummary().getTotalCount());

        // 전체 전적 요약 정보 조회
        MatchOverallSummaryDTO overallSummary = matchDbService.getOverallSummary(puuid);
        model.addAttribute("overallSummary", overallSummary);

//        // 선호 챔피언
//        Map<String, List<FavoriteChampionDTO>> favoriteChampionsMap = matchDbService.getFavoriteChampionsAll(puuid);
//        model.addAttribute("favoriteChampionsByMode", favoriteChampionsMap);

//        List<FavoriteChampionDTO> overall = matchDbService.getFavoriteChampions(puuid, "overall");
//        model.addAttribute("overallChampions", overall);
//        model.addAttribute("soloChampions", favoriteChampionsMap.get("solo"));
//        model.addAttribute("flexChampions", favoriteChampionsMap.get("flex"));

        // 선호 챔피언 - 전체 매치 데이터 기반으로 큐타입별 계산
        List<MatchHistoryDTO> allMatches = matchDbService.getAllMatchSummaryFromDB(puuid);

        // 전체 (모든 랭크 게임)
        List<MatchHistoryDTO> overallMatches = allMatches.stream()
                .filter(match -> "420".equals(match.getQueueType()) || "440".equals(match.getQueueType()))
                .collect(Collectors.toList());
        System.out.println("[DEBUG] 전체 탭 매치 수: " + overallMatches.size());
        List<FavoriteChampionDTO> overallChampions = matchDbService.calculateFavoriteChampions(overallMatches, "overall", puuid);
        System.out.println("[DEBUG] 전체 탭 챔피언 수: " + (overallChampions != null ? overallChampions.size() : 0));

        // 솔로랭크 (420)
        List<MatchHistoryDTO> soloMatches = allMatches.stream()
                .filter(match -> "420".equals(match.getQueueType()))
                .collect(Collectors.toList());
        System.out.println("[DEBUG] 솔로랭크 매치 수: " + soloMatches.size());
        List<FavoriteChampionDTO> soloChampions = matchDbService.calculateFavoriteChampions(soloMatches, "solo", puuid);
        System.out.println("[DEBUG] 솔로랭크 챔피언 수: " + (soloChampions != null ? soloChampions.size() : 0));

        // 자유랭크 (440)
        List<MatchHistoryDTO> flexMatches = allMatches.stream()
                .filter(match -> "440".equals(match.getQueueType()))
                .collect(Collectors.toList());
        System.out.println("[DEBUG] 자유랭크 매치 수: " + flexMatches.size());
        List<FavoriteChampionDTO> flexChampions = matchDbService.calculateFavoriteChampions(flexMatches, "flex", puuid);
        System.out.println("[DEBUG] 자유랭크 챔피언 수: " + (flexChampions != null ? flexChampions.size() : 0));
        System.out.println("=== DEBUG END ===");

        // null 체크 및 빈 리스트 처리
        model.addAttribute("overallChampions", overallChampions != null ? overallChampions : new ArrayList<>());
        model.addAttribute("soloChampions", soloChampions != null ? soloChampions : new ArrayList<>());
        model.addAttribute("flexChampions", flexChampions != null ? flexChampions : new ArrayList<>());

        // 챔피언별 전적 비율 계산
        int totalChampionGames = dto.getSummary().getTotalCount();
        Map<String, String> championHeightStyles = Optional.ofNullable(dto.getSummary().getChampionTotalGames())
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            int value = e.getValue() != null ? e.getValue() : 0;
                            double height = totalChampionGames > 0 ? (value * 100.0 / totalChampionGames) : 0.0;
                            return "height:" + height + "%";
                        }
                ));
        model.addAttribute("championHeightStyles", championHeightStyles);

        // 포지션별 전적 비율 계산
        Map<String, Double> favoritePositions = Optional.ofNullable(overallSummary.getFavoritePositions())
                .orElse(Collections.emptyMap());

        double totalPositionPercent = favoritePositions.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();


        Map<String, String> positionHeightStyles = favoritePositions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            double value = e.getValue() != null ? e.getValue() : 0.0;
                            double height = totalPositionPercent > 0 ? (value * 100.0 / totalPositionPercent) : 0.0;
                            return "height:" + height + "%";
                        }
                ));
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

        return "fo/matchHistory/matchHistory";
    }

    // 선호 챔피언 비동기 요청( 탭 눌러야 동기화 )
    @GetMapping("/favorite-champions")
    @ResponseBody
    public List<FavoriteChampionDTO> getFavoriteChampions(
            @RequestParam("puuid") String puuid,
            @RequestParam("mode") String mode) {
        return matchDbService.getFavoriteChampions(puuid, mode);
    }

    // 초기화 호출
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
        return matchDbService.getMatchDetailFromDb(matchId, puuid);
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

    // 테스트 - 테이블에 정보 넣기
    @GetMapping("/test-init")
    @ResponseBody
    public String testInit() { // 한꺼번에 초기화 시키면 키 제한 걸림
        log.info("testInit() 실행됨");

//        matchDbService.saveMatchSummaryAndPlayers("어리고싶다", "KR1", "MASTER");
//        matchDbService.saveMatchSummaryAndPlayers("96년생 티모장인", "9202", "MASTER");
//        matchDbService.saveMatchSummaryAndPlayers("허거덩", "0303", "DIAMOND1");
//        matchDbService.saveMatchSummaryAndPlayers("Hide on bush", "KR1", "DIAMOND1");
//        matchDbService.saveMatchSummaryAndPlayers("T1 Gumayusi", "KR1", "DIAMOND1");
//        matchDbService.saveMatchSummaryAndPlayers("Summer", "pado", "MASTER");
//        matchDbService.saveMatchSummaryAndPlayers("죽기장인", "KR1", "GRANDMASTER");
//        matchDbService.saveMatchSummaryAndPlayers("kiin", "KR1", "DIAMOND1");
//        matchDbService.saveMatchSummaryAndPlayers("귀찮게하지마", "KR3", "MASTER");
//        matchDbService.saveMatchSummaryAndPlayers("파피몬", "1111", "DIAMOND3");
//        matchDbService.saveMatchSummaryAndPlayers("파이리", "1217", "MASTER");

//        matchDbService.saveOnlyOverallSummary("어리고싶다", "KR1", "MASTER");
//        matchDbService.saveOnlyOverallSummary("96년생 티모장인", "9202", "MASTER");
//        matchDbService.saveOnlyOverallSummary("허거덩", "0303", "DIAMOND1");
//        matchDbService.saveOnlyOverallSummary("Hide on bush", "KR1", "DIAMOND1");
//        matchDbService.saveOnlyOverallSummary("T1 Gumayusi", "KR1", "DIAMOND1");
//        matchDbService.saveOnlyOverallSummary("Summer", "pado", "MASTER");
//        matchDbService.saveOnlyOverallSummary("죽기장인", "KR1", "GRANDMASTER");
//        matchDbService.saveOnlyOverallSummary("kiin", "KR1", "DIAMOND1");
//        matchDbService.saveOnlyOverallSummary("귀찮게하지마", "KR3", "MASTER");
//        matchDbService.saveOnlyOverallSummary("파피몬", "1111", "DIAMOND3");
//        matchDbService.saveOnlyOverallSummary("파이리", "1217", "MASTER");

//        matchDbService.saveFavoriteChampionOnly("어리고싶다", "KR1");
//        matchDbService.saveFavoriteChampionOnly("96년생 티모장인", "9202");
//        matchDbService.saveFavoriteChampionOnly("허거덩", "0303");
//        matchDbService.saveFavoriteChampionOnly("Hide on bush", "KR1");
//        matchDbService.saveFavoriteChampionOnly("T1 Gumayusi", "KR1");
//        matchDbService.saveFavoriteChampionOnly("Summer", "pado");
//        matchDbService.saveFavoriteChampionOnly("죽기장인", "KR1");
//        matchDbService.saveFavoriteChampionOnly("kiin", "KR1");
//        matchDbService.saveFavoriteChampionOnly("귀찮게하지마", "KR3");
//        matchDbService.saveFavoriteChampionOnly("파피몬", "1111");
//        matchDbService.saveFavoriteChampionOnly("파이리", "1217");

        return "전적 저장 완료";
    }


}
