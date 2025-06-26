package com.plit.FO.matchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/fo/match")
@RequiredArgsConstructor
public class MatchHistoryController {

    private final MatchHistoryService matchHistoryService;

    @GetMapping
    public String getMatchHistory(@RequestParam(name = "username") String username, Model model) {

        List<MatchHistoryDTO> matchHistoryDTOList = matchHistoryService.getMatchHistory(username);

        model.addAttribute("username", username);
        model.addAttribute("matchHistoryDTOList", matchHistoryDTOList);

        return "fo/matchHistory/matchHistory";
    }
}