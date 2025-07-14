package com.plit.FO.clan.controller;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.dto.ClanJoinRequestDTO;
import com.plit.FO.clan.dto.ClanMemberDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.service.ClanJoinRequestService;
import com.plit.FO.clan.service.ClanMemberService;
import com.plit.FO.clan.service.ClanService;
import com.plit.FO.user.service.UserService;
import com.plit.FO.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Controller
@RequestMapping("/clan")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;
    private final UserService userService;
    private final ClanMemberService clanMemberService;
    private final ClanJoinRequestService clanJoinRequestService;

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

//    @GetMapping
//    public String listClans(@RequestParam(required = false) String keyword,
//                            @RequestParam(required = false) String tier,
//                            Model model, Principal principal)  {
//        List<ClanEntity> clans = clanService.searchClansByKeywordAndTier(keyword, tier);
//        model.addAttribute("clans", clans);
//        model.addAttribute("tier", tier);
//
//        if (principal != null) {
//            String userId = principal.getName();
//            Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);
//
//            optionalUser.ifPresent(user -> model.addAttribute("nickname", user.getUserNickname()));
//        }
//
//        return "fo/clan/clan-list";
//    }

    @GetMapping
    public String listClans(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String tier,
                            Model model, Principal principal) {

        List<ClanEntity> clanEntities = clanService.searchClansByKeywordAndTier(keyword, tier);

        // ClanDTO에 memberCount 포함해서 변환
        List<ClanDTO> clanDTOs = clanEntities.stream()
                .map(clan -> {
                    int memberCount = clanMemberService.countByClanId(clan.getId());
                    return ClanDTO.builder()
                            .id(clan.getId())
                            .name(clan.getName())
                            .intro(clan.getIntro())
                            .imageUrl(clan.getImageUrl())
                            .minTier(clan.getMinTier())
                            .kakaoLink(clan.getKakaoLink())
                            .discordLink(clan.getDiscordLink())
                            .leaderId(clan.getLeaderId())
                            .memberCount(memberCount)
                            .build();
                })
                .toList();

        model.addAttribute("clans", clanDTOs);
        model.addAttribute("tier", tier);

        if (principal != null) {
            String userId = principal.getName();
            Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);
            optionalUser.ifPresent(user -> model.addAttribute("nickname", user.getUserNickname()));
        }

        return "fo/clan/clan-list";
    }

    @PostMapping("/register")
    public String registerClan(@ModelAttribute ClanEntity clan,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        String userId = principal.getName();
        Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        UserDTO loginUser = optionalUser.get();
        clan.setLeaderId(loginUser.getUserSeq().longValue());

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                imageFile.transferTo(new File(dir, fileName));
                clan.setImageUrl("/uploads/clan/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                clan.setImageUrl("/images/clan/clan_default.png");
            }
        } else {
            clan.setImageUrl("/images/clan/clan_default.png");
        }

        clanService.createClan(clan);
        return "redirect:/clan";
    }

    @GetMapping("/{id}")
    public String clanDetail(@PathVariable Long id, Model model, Principal principal) {
        ClanDTO clan = clanService.findById(id);
        model.addAttribute("clan", clan);

        List<ClanMemberDTO> members = clanMemberService.findApprovedMembersByClanId(id);
        List<ClanJoinRequestDTO> pendingMembers = clanJoinRequestService.getJoinRequests(id);

        // 리더 정보 추가
        if (clan.getLeaderId() != null) {
            clanMemberService.findByClanIdAndUserId(id, clan.getLeaderId()).ifPresent(leaderDto -> {
                leaderDto.setRole("LEADER");
                leaderDto.setIntro(leaderDto.getIntro() != null ? leaderDto.getIntro() : "리더입니다");
                boolean leaderExists = members.stream()
                        .anyMatch(m -> m.getMemberId().equals(leaderDto.getMemberId()));
                if (!leaderExists) {
                    members.add(0, leaderDto);
                }
            });
        }

        model.addAttribute("members", members);
        model.addAttribute("pendingMembers", pendingMembers);

        // 로그인 유저일 경우
        if (principal != null) {
            String userIdStr = principal.getName();
            userService.getUserByUserId(userIdStr).ifPresent(userDTO -> {
                Long userSeq = userDTO.getUserSeq().longValue();
                model.addAttribute("nickname", userDTO.getUserNickname());

                boolean isJoinPending = clanJoinRequestService.isJoinPending(id, userSeq);

                clanMemberService.findByClanIdAndUserId(id, userSeq).ifPresentOrElse(
                        memberDto -> {
                            model.addAttribute("editMember", memberDto);
                            String role = memberDto.getRole();
                            if ("LEADER".equals(role) || "MEMBER".equals(role)) {
                                model.addAttribute("role", role);
                            } else {
                                model.addAttribute("role", "GUEST");
                                model.addAttribute("joinPending", isJoinPending);
                            }
                        },
                        () -> {
                            model.addAttribute("role", "GUEST");
                            model.addAttribute("joinPending", isJoinPending);
                        }
                );
            });

            if (!model.containsAttribute("role") || model.getAttribute("role") == null) {
                model.addAttribute("role", "GUEST");
            }

        } else {
            // 로그인 안한 사용자
            model.addAttribute("role", "GUEST");
            model.addAttribute("joinPending", false);
        }

        return "fo/clan/clan-detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteClan(@PathVariable Long id) {
        clanService.deleteClan(id);
        return "redirect:/clan";
    }

    @GetMapping("/check-name")
    @ResponseBody
    public boolean checkClanName(@RequestParam String name) {
        return !clanService.existsByNameAndUseYn(name, "Y");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "이미지 용량이 너무 큽니다! 10MB 이하로 업로드해주세요.");
        return "redirect:/clan/register";
    }

    @PostMapping("/edit/{id}")
    public String updateClan(@PathVariable Long id,
                             @ModelAttribute ClanEntity updatedClan,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             RedirectAttributes redirectAttributes) {

        try {
            clanService.updateClan(id, updatedClan, imageFile);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "클랜 수정 중 오류가 발생했습니다.");
            return "redirect:/clan/" + id;
        }

        return "redirect:/clan/" + id;
    }

    @PostMapping("/member/update")
    @ResponseBody
    public ResponseEntity<String> updateMemberInfo(Principal principal,
                                                   @RequestBody ClanMemberDTO dto) {
        try {
            // 주 포지션 선택 확인
            String mainPosition = dto.getMainPosition();
            if (mainPosition == null || mainPosition.trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("주 포지션을 선택해주세요.");
            }

            // intro 길이 검사
            String intro = dto.getIntro();
            if (intro != null && intro.length() > 30) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("자기소개는 30자 이하로 입력해주세요.");
            }

            // 로그인한 사용자 ID 가져오기
            String userId = principal.getName();

            // DB에서 실제 유저 정보 조회
            UserDTO userDTO = userService.findByUserId(userId);
            Long realUserId = userDTO.getUserSeq().longValue();

            // 요청에서 클랜 ID 꺼내기
            Long clanId = dto.getClanId();

            // 서비스 호출
            clanMemberService.updateMemberInfo(realUserId, clanId, dto);

            return ResponseEntity.ok("멤버 정보가 수정되었습니다");
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔 로그
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("멤버 정보 수정 실패");
        }
    }

    // 클랜 가입
    @PostMapping("/join")
    public ResponseEntity<Void> joinClan(@RequestBody ClanJoinRequestDTO dto, Principal principal) {
        String userId = principal.getName();
        UserDTO userDTO = userService.findByUserId(userId);
        dto.setUserId(userDTO.getUserSeq().longValue());

        clanJoinRequestService.requestJoin(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{clanId}/approve/{userId}")
    @ResponseBody
    public ResponseEntity<String> approveJoinRequest(@PathVariable Long clanId,
                                                     @PathVariable Long userId,
                                                     Principal principal) {
        try {
            String requesterUserId = principal.getName();
            UserDTO loginUser = userService.findByUserId(requesterUserId);
            Long loginUserSeq = loginUser.getUserSeq().longValue();
            ClanDTO clan = clanService.findById(clanId);


            if (!clan.getLeaderId().equals(loginUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 수락할 수 있습니다.");
            }

            clanJoinRequestService.approveJoinRequest(clanId, userId);
            return ResponseEntity.ok("가입 신청이 수락되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("수락 실패: " + e.getMessage());
        }
    }

    @PostMapping("/{clanId}/reject/{userId}")
    @ResponseBody
    public ResponseEntity<String> rejectJoinRequest(@PathVariable Long clanId,
                                                    @PathVariable Long userId,
                                                    Principal principal) {
        try {
            String requesterUserId = principal.getName();
            UserDTO loginUser = userService.findByUserId(requesterUserId);
            Long loginUserSeq = loginUser.getUserSeq().longValue();

            ClanDTO clan = clanService.findById(clanId);

            if (!clan.getLeaderId().equals(loginUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 거절할 수 있습니다.");
            }

            clanJoinRequestService.rejectJoinRequest(clanId, userId);
            return ResponseEntity.ok("가입 신청이 거절되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("거절 실패: " + e.getMessage());
        }
    }
}
