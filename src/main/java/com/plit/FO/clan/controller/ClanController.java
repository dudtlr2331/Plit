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

    @GetMapping
    public String listClans(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String tier,
                            Model model, Principal principal) {
        try {
            List<ClanEntity> clanEntities = clanService.searchClansByKeywordAndTier(keyword, tier);
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
        } catch (Exception e) {
            model.addAttribute("errorMessage", "클랜 목록을 불러오는 중 문제가 발생했습니다.");
            return "error/common-error";
        }
    }

    @PostMapping("/register")
    public String registerClan(@ModelAttribute ClanEntity clan,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        try {
            if (principal == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            String userId = principal.getName();
            Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);
            if (optionalUser.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인 정보를 찾을 수 없습니다.");
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
                    clan.setImageUrl("/upload/clan/" + fileName);
                } catch (IOException e) {
                    clan.setImageUrl("/images/clan/clan_default.png");
                }
            } else {
                clan.setImageUrl("/images/clan/clan_default.png");
            }

            clanService.createClan(clan);
            return "redirect:/clan";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "클랜 등록 중 오류가 발생했습니다.");
            return "redirect:/clan";
        }
    }

    @GetMapping("/{id}")
    public String clanDetail(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            ClanDTO clan = clanService.findById(id);
            model.addAttribute("clan", clan);

            List<ClanMemberDTO> members = clanMemberService.findApprovedMembersByClanId(id);
            List<ClanJoinRequestDTO> pendingMembers = clanJoinRequestService.getJoinRequests(id);

            if (clan.getLeaderId() != null) {
                clanMemberService.findByClanIdAndUserId(id, clan.getLeaderId()).ifPresent(leaderDto -> {
                    leaderDto.setRole("LEADER");
                    leaderDto.setIntro(leaderDto.getIntro() != null ? leaderDto.getIntro() : "리더입니다");

                    boolean alreadyInList = members.stream()
                            .anyMatch(m -> m.getUserId() != null && m.getUserId().equals(leaderDto.getUserId()));

                    if (!alreadyInList) {
                        members.add(leaderDto);
                    }
                });
            }

            Long leaderId = clan.getLeaderId();
            AtomicReference<Long> currentUserMemberId = new AtomicReference<>(null);

            if (principal != null) {
                String userIdStr = principal.getName();
                userService.getUserByUserId(userIdStr).ifPresent(userDTO -> {
                    Long userSeq = userDTO.getUserSeq().longValue();
                    clanMemberService.findByClanIdAndUserId(id, userSeq).ifPresent(currentMemberDto -> {
                        currentUserMemberId.set(currentMemberDto.getMemberId());
                    });
                });
            }

            members.sort((m1, m2) -> {
                Long m1Id = m1.getMemberId();
                Long m2Id = m2.getMemberId();
                Long currentId = currentUserMemberId.get();

                if (m1Id.equals(leaderId)) return -1;
                if (m2Id.equals(leaderId)) return 1;

                if (currentId != null) {
                    if (m1Id.equals(currentId)) return -1;
                    if (m2Id.equals(currentId)) return 1;
                }

                return 0;
            });

            model.addAttribute("members", members);
            model.addAttribute("pendingMembers", pendingMembers);
            model.addAttribute("pendingCount", pendingMembers.size());

            if (principal != null) {
                String userIdStr = principal.getName();
                userService.getUserByUserId(userIdStr).ifPresent(userDTO -> {
                    Long userSeq = userDTO.getUserSeq().longValue();
                    model.addAttribute("nickname", userDTO.getUserNickname());

                    boolean isJoinPending = clanJoinRequestService.isJoinPending(id, userSeq);
                    model.addAttribute("currentUserId", userDTO.getUserSeq());

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
                model.addAttribute("role", "GUEST");
                model.addAttribute("joinPending", false);
            }

            return "fo/clan/clan-detail";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "클랜 상세 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/clan";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteClan(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            if (principal == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
                return "redirect:/login";
            }

            String userId = principal.getName();
            UserDTO user = userService.findByUserId(userId);
            ClanDTO clan = clanService.findById(id);

            if (clan == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "클랜을 찾을 수 없습니다.");
                return "redirect:/clan";
            }

            if (!clan.getLeaderId().equals(user.getUserSeq().longValue())) {
                redirectAttributes.addFlashAttribute("errorMessage", "리더만 삭제할 수 있습니다.");
                return "redirect:/clan/" + id;
            }

            clanService.deleteClan(id);
            redirectAttributes.addFlashAttribute("successMessage", "클랜이 삭제되었습니다.");
            return "redirect:/clan";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "클랜 삭제 중 오류가 발생했습니다.");
            return "redirect:/clan/" + id;
        }
    }

    @GetMapping("/check-name")
    @ResponseBody
    public boolean checkClanName(@RequestParam String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return false;
            }
            return !clanService.existsByNameAndUseYn(name.trim(), "Y");
        } catch (Exception e) {
            return false;
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException e, RedirectAttributes redirectAttributes) {
        System.err.println("파일 업로드 용량 초과: " + e.getMessage());
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
        } catch (IOException ioEx) {
            System.err.println("파일 업로드 오류: " + ioEx.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "이미지 업로드에 실패했습니다.");
            return "redirect:/clan/" + id;
        } catch (NullPointerException npe) {
            System.err.println("Null 값 오류: " + npe.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "입력 값이 올바르지 않습니다.");
            return "redirect:/clan/" + id;
        } catch (Exception e) {
            System.err.println("클랜 수정 오류: " + e.getMessage());
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
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            if (dto.getPosition() == null || dto.getPosition().getLabel() == null || dto.getPosition().getLabel().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("주 포지션을 선택해주세요.");
            }

            String intro = dto.getIntro();
            if (intro != null && intro.length() > 30) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("자기소개는 30자 이하로 입력해주세요.");
            }

            String userId = principal.getName();
            UserDTO userDTO = userService.findByUserId(userId);
            if (userDTO == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 정보를 찾을 수 없습니다.");
            }

            Long realUserId = userDTO.getUserSeq().longValue();
            Long clanId = dto.getClanId();

            if (clanId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("클랜 ID가 유효하지 않습니다.");
            }

            clanMemberService.updateMemberInfo(realUserId, clanId, dto);
            return ResponseEntity.ok("멤버 정보가 수정되었습니다.");
        } catch (Exception e) {
            System.err.println("멤버 정보 수정 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("멤버 정보 수정 중 문제가 발생했습니다.");
        }
    }

    // 클랜 가입
    @PostMapping("/join")
    public ResponseEntity<String> joinClan(@RequestBody ClanJoinRequestDTO dto, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String userId = principal.getName();
            UserDTO userDTO = userService.findByUserId(userId);
            if (userDTO == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            dto.setUserId(userDTO.getUserSeq().longValue());

            if (dto.getClanId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("클랜 ID가 유효하지 않습니다.");
            }

            clanJoinRequestService.requestJoin(dto);
            return ResponseEntity.ok("가입 요청이 정상적으로 처리되었습니다.");
        } catch (Exception e) {
            System.err.println("클랜 가입 요청 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("클랜 가입 요청 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/{clanId}/approve/{userId}")
    @ResponseBody
    public ResponseEntity<String> approveJoinRequest(@PathVariable Long clanId,
                                                     @PathVariable Long userId,
                                                     Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String requesterUserId = principal.getName();
            UserDTO loginUser = userService.findByUserId(requesterUserId);
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            Long loginUserSeq = loginUser.getUserSeq().longValue();
            ClanDTO clan = clanService.findById(clanId);
            if (clan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 클랜을 찾을 수 없습니다.");
            }

            if (!clan.getLeaderId().equals(loginUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 수락할 수 있습니다.");
            }

            clanJoinRequestService.approveJoinRequest(clanId, userId);
            return ResponseEntity.ok("가입 신청이 수락되었습니다.");
        } catch (Exception e) {
            System.err.println("가입 수락 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("수락 실패: 서버 오류 발생");
        }
    }

    @PostMapping("/{clanId}/reject/{userId}")
    @ResponseBody
    public ResponseEntity<String> rejectJoinRequest(@PathVariable Long clanId,
                                                    @PathVariable Long userId,
                                                    Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String requesterUserId = principal.getName();
            UserDTO loginUser = userService.findByUserId(requesterUserId);
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            Long loginUserSeq = loginUser.getUserSeq().longValue();
            ClanDTO clan = clanService.findById(clanId);
            if (clan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 클랜을 찾을 수 없습니다.");
            }

            if (!clan.getLeaderId().equals(loginUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 거절할 수 있습니다.");
            }

            clanJoinRequestService.rejectJoinRequest(clanId, userId);
            return ResponseEntity.ok("가입 신청이 거절되었습니다.");
        } catch (Exception e) {
            System.err.println("가입 거절 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("거절 실패: 서버 오류 발생");
        }
    }

    // 리더 위임
    @PostMapping("/{clanId}/delegate/{toUserSeq}")
    @ResponseBody
    public ResponseEntity<String> delegateLeader(@PathVariable Long clanId,
                                                 @PathVariable Long toUserSeq,
                                                 Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String loginId = principal.getName();
            UserDTO loginUser = userService.findByUserId(loginId);
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            Long fromUserSeq = loginUser.getUserSeq().longValue();

            ClanDTO clan = clanService.findById(clanId);
            if (clan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 클랜을 찾을 수 없습니다.");
            }

            if (!clan.getLeaderId().equals(fromUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 위임할 수 있습니다.");
            }

            clanMemberService.delegateLeader(clanId, fromUserSeq, toUserSeq);
            return ResponseEntity.ok("리더 위임 완료!");
        } catch (Exception e) {
            System.err.println("리더 위임 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("리더 위임 실패: 서버 오류 발생");
        }
    }

    // 클랜 추방
    @PostMapping("/{clanId}/kick/{targetUserSeq}")
    @ResponseBody
    public ResponseEntity<String> kickMember(@PathVariable Long clanId,
                                             @PathVariable Long targetUserSeq,
                                             Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String loginId = principal.getName();
            UserDTO loginUser = userService.findByUserId(loginId);
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            Long loginUserSeq = loginUser.getUserSeq().longValue();

            ClanDTO clan = clanService.findById(clanId);
            if (clan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 클랜을 찾을 수 없습니다.");
            }

            if (!clan.getLeaderId().equals(loginUserSeq)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("리더만 멤버를 추방할 수 있습니다.");
            }

            clanMemberService.kickMember(clanId, loginUserSeq, targetUserSeq);
            return ResponseEntity.ok("멤버 추방 완료!");
        } catch (Exception e) {
            System.err.println("멤버 추방 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("추방 실패: 서버 오류 발생");
        }
    }

    // 클랜 탈퇴
    @PostMapping("/{clanId}/leave")
    @ResponseBody
    public ResponseEntity<String> leaveClan(@PathVariable Long clanId, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            String userIdStr = principal.getName();
            Optional<UserDTO> optionalUser = userService.getUserByUserId(userIdStr);

            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
            }

            Long userSeq = optionalUser.get().getUserSeq().longValue();

            ClanDTO clan = clanService.findById(clanId);
            if (clan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("클랜이 존재하지 않습니다.");
            }

            clanMemberService.leaveClan(clanId, userSeq);
            return ResponseEntity.ok("클랜 탈퇴 완료!");
        } catch (Exception e) {
            System.err.println("클랜 탈퇴 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("클랜 탈퇴 중 오류가 발생했습니다.");
        }
    }
}
