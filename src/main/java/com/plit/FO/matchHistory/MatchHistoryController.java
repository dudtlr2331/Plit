package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("summoner", summoner);

        return "fo/matchHistory/matchHistory";
    }
}
