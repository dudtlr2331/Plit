package com.plit.FO.party.controller;

import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.service.PartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/parties")
public class PartyRestController {

    private final PartyService partyService;

    public PartyRestController(PartyService partyService) {
        this.partyService = partyService;
    }

    // 파티 목록 조회 (타입별)
    @GetMapping
    public ResponseEntity<List<PartyDTO>> getParties(@RequestParam(defaultValue = "solo") String partyType) {
        List<PartyDTO> list = partyService.findByPartyType(partyType);
        return ResponseEntity.ok(list);
    }

    // 파티 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PartyDTO> getParty(@PathVariable Long id) {
        try {
            PartyDTO dto = partyService.getParty(id);
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 파티 생성 (로그인 유저 기반)
    @PostMapping
    public ResponseEntity<Void> createParty(@RequestBody PartyDTO dto,
                                            @AuthenticationPrincipal User user) {
        partyService.saveParty(dto, user.getUsername());
        return ResponseEntity.ok().build();
    }

    // 파티 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateParty(@PathVariable Long id,
                                            @RequestBody PartyDTO dto) {
        partyService.updateParty(id, dto);
        return ResponseEntity.ok().build();
    }

    // 파티 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long id) {
        partyService.deleteParty(id);
        return ResponseEntity.noContent().build();
    }
}