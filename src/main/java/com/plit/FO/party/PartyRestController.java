package com.plit.FO.party;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
public class PartyRestController {

    private final PartyService partyService;

    public PartyRestController(PartyService partyService) {
        this.partyService = partyService;
    }

    @GetMapping
    public List<PartyDTO> getParties(@RequestParam(defaultValue = "솔로랭크") String partyType) {
        return partyService.findByPartyType(partyType);
    }

//    private final PartyService partyService;
//
//    public PartyRestController(PartyService partyService) {
//        this.partyService = partyService;
//    }
//
//    @GetMapping
//    public List<PartyEntity> listAll() {
//        return partyService.findAllParties();
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<PartyEntity> getParty(@PathVariable Long id) {
//        PartyEntity party = partyService.getParty(id);
//        return party != null ? ResponseEntity.ok(party) : ResponseEntity.notFound().build();
//    }
//
//    @PostMapping
//    public PartyEntity createParty(@RequestBody PartyEntity party) {
//        return partyService.saveParty(party);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<PartyEntity> updateParty(@PathVariable Long id, @RequestBody PartyEntity partyData) {
//        PartyEntity existing = partyService.getParty(id);
//        if (existing == null) {
//            return ResponseEntity.notFound().build();
//        }
//        existing.setPartyName(partyData.getPartyName());
//        existing.setPartyType(partyData.getPartyType());
//        existing.setPartyCreateDate(partyData.getPartyCreateDate());
//        existing.setPartyEndTime(partyData.getPartyEndTime());
//        existing.setPartyStatus(partyData.getPartyStatus());
//        existing.setPartyHeadcount(partyData.getPartyHeadcount());
//        existing.setPartyMax(partyData.getPartyMax());
//        PartyEntity updated = partyService.saveParty(existing);
//        return ResponseEntity.ok(updated);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteParty(@PathVariable Long id) {
//        PartyEntity existing = partyService.getParty(id);
//        if (existing == null) {
//            return ResponseEntity.notFound().build();
//        }
//        partyService.deleteParty(id);
//        return ResponseEntity.noContent().build();
//    }
}
