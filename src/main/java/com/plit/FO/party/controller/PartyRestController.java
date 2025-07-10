package com.plit.FO.party.controller;

import com.plit.FO.party.dto.JoinRequestDTO;
import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.dto.PartyMemberDTO;
import com.plit.FO.party.repository.PartyMemberRepository;
import com.plit.FO.party.service.PartyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public ResponseEntity<String> createParty(@RequestBody PartyDTO dto,
                                              @AuthenticationPrincipal Object principal) {
        String userId = null;

        if (principal instanceof UserDetails userDetails) {
            userId = userDetails.getUsername();
        } else if (principal instanceof OAuth2User oauthUser) {
            userId = oauthUser.getAttribute("email"); // 또는 "id", "nickname"
        }

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 서비스입니다.");
        }

        partyService.saveParty(dto, userId);
        return ResponseEntity.ok("OK");
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

        try {
            partyService.joinParty(partyId, user.getUsername(), request.getPosition(), request.getMessage());
            return ResponseEntity.ok("OK");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 파티 참가 체크
    @GetMapping("/{partyId}/joined")
    public boolean checkIfJoined(@PathVariable Long partyId,
                                 @AuthenticationPrincipal(expression = "username") String userId) {
        return partyMemberRepository.existsByParty_PartySeqAndUserIdAndStatus(partyId, userId, "ACCEPTED");
    }

    // 파티에 참가 중인 멤버 목록
    @GetMapping("/{partyId}/members")
    public ResponseEntity<List<PartyMemberDTO>> getPartyMembers(@PathVariable Long partyId) {
        List<PartyMemberDTO> members = partyService.getPartyMemberDTOs(partyId);
        return ResponseEntity.ok(members);
    }

    //파티 참가 수락
    @PostMapping("/{partyId}/members/{memberId}/accept")
    public ResponseEntity<?> acceptMember(@PathVariable Long partyId, @PathVariable Long memberId) {
        try {
            partyService.acceptMember(partyId, memberId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 파티 참가 거절
    @PostMapping("/{partyId}/members/{memberId}/reject")
    public ResponseEntity<Void> rejectMember(@PathVariable Long partyId, @PathVariable Long memberId) {
        partyService.rejectMember(partyId, memberId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{partyId}/join-status")
    public ResponseEntity<String> getJoinStatus(@PathVariable Long partyId,
                                                @AuthenticationPrincipal(expression = "username") String userId) {
        String status = partyService.getJoinStatus(partyId, userId); // NONE, PENDING, ACCEPTED, REJECTED
        return ResponseEntity.ok(status);
    }
}