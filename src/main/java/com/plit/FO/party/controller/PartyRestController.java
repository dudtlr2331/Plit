package com.plit.FO.party.controller;

import com.plit.FO.party.dto.*;
import com.plit.FO.party.enums.MemberStatus;
import com.plit.FO.party.repository.PartyMemberRepository;
import com.plit.FO.party.service.PartyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
                                            @RequestBody PartyDTO dto,
                                            @AuthenticationPrincipal UserDetails user) {
        partyService.updateParty(id, dto, user.getUsername());
        return ResponseEntity.ok().build();
    }

    // 파티 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails user) {
        partyService.deleteParty(id, user.getUsername());
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
        return partyMemberRepository.existsByParty_PartySeqAndUser_UserIdAndStatus(partyId, userId, MemberStatus.ACCEPTED);
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
        MemberStatus status = partyService.getJoinStatus(partyId, userId);
        String result = status != null ? status.name() : "NONE";
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{partyId}/members/{memberId}/kick")
    public ResponseEntity<String> kickMember(@PathVariable Long partyId, @PathVariable Long memberId, Principal principal) {
        partyService.kickMember(partyId, memberId, principal.getName());
        return ResponseEntity.ok("KICKED");
    }

    /* 파티 나가기 */
    @DeleteMapping("/{partyId}/members/leave")
    public ResponseEntity<String> leaveParty(@PathVariable Long partyId,
                                             @AuthenticationPrincipal(expression = "username") String userId) {
        partyService.leaveParty(partyId, userId);
        return ResponseEntity.ok("LEAVED");
    }

    @PostMapping("/{partyId}/scrim-join")
    public ResponseEntity<String> joinScrimTeam(
            @PathVariable Long partyId,
            @RequestBody ScrimJoinRequestDTO request) {

        try {
            partyService.joinScrimTeam(partyId, request);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.out.println("예외 발생!");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("실패: " + e.getMessage());
        }
    }

    @PostMapping("/scrim-create")
    public ResponseEntity<String> createScrimParty(@RequestBody ScrimCreateRequestDTO request,
                                                   @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요한 기능입니다.");
        }

        try {
            partyService.createScrimParty(request, user.getUsername());
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("생성 실패: " + e.getMessage());
        }
    }

    /* 내전찾기 팀 수락 거절*/
    @PostMapping("/{partyId}/members/approve-team")
    public ResponseEntity<?> approveTeam(@PathVariable Long partyId,
                                         @RequestBody TeamApprovalRequestDTO request) {
        partyService.approveTeam(partyId, request.getMemberIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{partyId}/members/reject-team")
    public ResponseEntity<?> rejectTeam(@PathVariable Long partyId,
                                        @RequestBody TeamApprovalRequestDTO request) {
        partyService.rejectTeam(partyId, request.getMemberIds());
        return ResponseEntity.ok().build();
    }
}