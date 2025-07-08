package com.plit.FO.party.controller;

import com.plit.FO.party.service.PartyService;
import com.plit.FO.party.dto.PartyDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/party")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    // 파티 목록 페이지
    @GetMapping
    public String partyPage() {
        return "fo/party/party";
    }

    // 파티 생성
    @PostMapping("/new")
    public String create(@AuthenticationPrincipal User user, PartyDTO dto) {
        partyService.saveParty(dto, user.getUsername());
        return "redirect:/party";
    }

    // 파티 수정
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, PartyDTO dto) {
        partyService.updateParty(id, dto);
        return "redirect:/party";
    }

    // 파티 삭제
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        partyService.deleteParty(id);
        return "redirect:/party";
    }
}