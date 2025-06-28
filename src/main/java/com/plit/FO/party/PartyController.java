package com.plit.FO.party;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/party")
public class PartyController {
    private final PartyService partyService;
    private final PartyRepository partyRepository;

    public PartyController(PartyService partyService, PartyRepository partyRepository) {
        this.partyService = partyService;
        this.partyRepository = partyRepository;
    }

    @GetMapping()
    public String list(Model model) {
        model.addAttribute("parties", partyService.getAllParties());
        return "fo/party/party";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("party", new PartyDTO());
        model.addAttribute("positions", PositionEnum.values());
        return "fo/party/form";
    }

    @PostMapping("/new")
    public String create(PartyDTO dto) {
        partyService.saveParty(dto);
        return "redirect:/party";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("party", partyService.getParty(id));
        return "fo/party/view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("party", partyService.getParty(id));
        model.addAttribute("positions", PositionEnum.values());
        return "fo/party/form";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, PartyDTO dto) {
        partyService.updateParty(id, dto);
        return "redirect:/party";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        partyRepository.deleteById(id);
        return "redirect:/party";
    }
}
