package com.plit.FO.party;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat; // 추가

import java.time.LocalDateTime; // 추가

@Controller
@RequestMapping("/party")
public class PartyController {

    private final PartyService partyService;

    @Autowired
    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping("/list")
    public String listParties(Model model) {
        model.addAttribute("parties", partyService.findAllParties());
        return "fo/party/list";
    }

    @GetMapping("/{id}")
    public String viewParty(@PathVariable Long id, Model model) {
        PartyEntity party = partyService.getParty(id);
        if (party == null) {
            return "redirect:/party/list";
        }
        model.addAttribute("party", party);
        return "fo/party/view";
    }

    @GetMapping("/new")
    public String newPartyForm(Model model) {
        // 새 파티 생성 시 현재 시간을 기본값으로 설정 (선택 사항)
        PartyEntity newParty = new PartyEntity();
        newParty.setPartyCreateDate(LocalDateTime.now());
        model.addAttribute("party", newParty);
        return "fo/party/form";
    }

    @PostMapping("/new")
    public String createParty(PartyEntity party) {
        // 생성일자가 폼에서 넘어오지 않는 경우, 여기서 설정 (선택 사항)
        if (party.getPartyCreateDate() == null) {
            party.setPartyCreateDate(LocalDateTime.now());
        }
        partyService.saveParty(party);
        return "redirect:/party/list";
    }

    @GetMapping("/edit/{id}")
    public String editPartyForm(@PathVariable Long id, Model model) {
        PartyEntity party = partyService.getParty(id);
        if (party == null) {
            return "redirect:/party/list";
        }
        model.addAttribute("party", party);
        return "fo/party/form";
    }

    // --- 이 부분이 핵심 수정 ---
    @PostMapping("/edit/{id}")
    public String updateParty(
            @PathVariable Long id,
            @ModelAttribute PartyEntity formData // @ModelAttribute 명시
    ) {
        PartyEntity existing = partyService.getParty(id);
        if (existing != null) {
            // 수동으로 모든 필드 설정
            existing.setPartyName(formData.getPartyName());
            existing.setPartyType(formData.getPartyType());
            // 생성일자는 수정 시 변경하지 않는 것이 일반적 (필요하면 변경)
            // existing.setPartyCreateDate(formData.getPartyCreateDate());
            existing.setPartyEndTime(formData.getPartyEndTime());
            existing.setPartyStatus(formData.getPartyStatus());
            existing.setPartyHeadcount(formData.getPartyHeadcount());
            existing.setPartyMax(formData.getPartyMax());
            partyService.saveParty(existing);
        }
        return "redirect:/party/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteParty(@PathVariable Long id) {
        partyService.deleteParty(id);
        return "redirect:/party/list";
    }
}
