package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/fo/match")
@RequiredArgsConstructor
public class MatchHistoryController {

    private final MatchHistoryService matchHistoryService;

    @GetMapping
    public String getMatchPage(@RequestParam String gameName,
                               @RequestParam String tagLine,
                               Model model) {

        SummonerDTO summoner = matchHistoryService.getAccountByRiotId(gameName, tagLine);
        System.out.println("summoner: " + summoner);

        List<MatchHistoryDTO> matchList = matchHistoryService.getMatchHistory(summoner.getPuuid());

        model.addAttribute("summoner", summoner);

        int winCount = (int) matchList.stream().filter(MatchHistoryDTO::isWin).count();
        int totalCount = matchList.size();

        model.addAttribute("matchList", matchList);
        model.addAttribute("winCount", winCount);
        model.addAttribute("totalCount", totalCount);

        Map<String, String> modeMap = Map.of(
                "CHERRY", "아레나",
                "CLASSIC", "소환사의 협곡",
                "ARAM", "칼바람 나락",
                "TUTORIAL", "튜토리얼"
        );

        model.addAttribute("modeMap", modeMap);

        return "fo/matchHistory/matchHistory";
    }
}
