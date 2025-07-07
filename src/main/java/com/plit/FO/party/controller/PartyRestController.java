package com.plit.FO.party.controller;

import com.plit.FO.party.dto.JoinRequestDTO;
import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.repository.PartyMemberRepository;
import com.plit.FO.party.service.PartyService;
import org.springframework.http.HttpStatus;
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
    private final PartyMemberRepository partyMemberRepository;

    public PartyRestController(PartyService partyService, PartyMemberRepository partyMemberRepository) {
        this.partyService = partyService;
        this.partyMemberRepository = partyMemberRepository;
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

    // 파티 참가
    @PostMapping("/{partyId}/join")
    public ResponseEntity<String> joinParty(@PathVariable Long partyId,
                                            @AuthenticationPrincipal User user,
                                            @RequestBody JoinRequestDTO request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // ✅ 로그 출력
        System.out.println(">>> [Join] userId = " + user.getUsername());
        System.out.println(">>> [Join] partyId = " + partyId);
        System.out.println(">>> [Join] position = " + request.getPosition());
        System.out.println(">>> [Join] message = " + request.getMessage());

        partyService.joinParty(partyId, user.getUsername(), request.getPosition(), request.getMessage());

        // DB에 저장됐는지 확인용 (transaction이 커밋되면 로그가 나오기 전이어도 INSERT 되어야 함)
        System.out.println(">>> [Join] joinParty() 완료");

        return ResponseEntity.ok("OK");
    }

    // 파티 참가 체크
    @GetMapping("/{partyId}/joined")
    public boolean checkIfJoined(@PathVariable Long partyId,
                                 @AuthenticationPrincipal(expression = "username") String userId) {
        return partyMemberRepository.existsByParty_PartySeqAndUserId(partyId, userId);
    }

    // 파티에 참가 중인 멤버 목록
    @GetMapping("/{partyId}/members")
    public ResponseEntity<List<String>> getPartyMembers(@PathVariable Long partyId) {
        List<String> members = partyService.getPartyMembers(partyId);
        return ResponseEntity.ok(members);
    }
}