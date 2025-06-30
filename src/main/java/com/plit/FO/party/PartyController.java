package com.plit.FO.party;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/party")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping
    public String partyPage() {
        return "fo/party/party";
    }

    @PostMapping("/new")
    public String create(PartyDTO dto) {
        partyService.saveParty(dto);
        return "redirect:/party";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, PartyDTO dto) {
        partyService.updateParty(id, dto);
        return "redirect:/party";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        partyService.deleteParty(id);
        return "redirect:/party";
    }
}