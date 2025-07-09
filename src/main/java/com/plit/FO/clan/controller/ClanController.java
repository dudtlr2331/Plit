package com.plit.FO.clan.controller;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.dto.ClanMemberDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.service.ClanMemberService;
import com.plit.FO.clan.service.ClanService;
import com.plit.FO.user.service.UserService;
import com.plit.FO.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    @GetMapping
    public String listClans(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String tier,
                            Model model, Principal principal)  {
        List<ClanEntity> clans = clanService.searchClansByKeywordAndTier(keyword, tier);
        model.addAttribute("clans", clans);
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
        List<ClanMemberDTO> pendingMembers = clanMemberService.findPendingMembersByClanId(id);

        // 리더 정보 가져오기 (중복 체크 포함)
        if (clan.getLeaderId() != null) {
            clanMemberService.findByClanIdAndUserId(id, clan.getLeaderId()).ifPresent(leaderDto -> {
                leaderDto.setRole("LEADER");
                leaderDto.setIntro(leaderDto.getIntro() != null ? leaderDto.getIntro() : "리더입니다 👑");

                boolean leaderExists = members.stream()
                        .anyMatch(m -> m.getMemberId().equals(leaderDto.getMemberId()));

                if (!leaderExists) {
                    members.add(0, leaderDto);
                }
            });
        }

        model.addAttribute("members", members);
        model.addAttribute("pendingMembers", pendingMembers);

        // 로그인한 멤버 정보와 권한 한 번에 처리
        if (principal != null) {
            String userIdStr = principal.getName();
            userService.getUserByUserId(userIdStr).ifPresent(userDTO -> {
                Long userSeq = userDTO.getUserSeq().longValue();

                clanMemberService.findByClanIdAndUserId(id, userSeq).ifPresent(memberDto -> {
                    model.addAttribute("editMember", memberDto);  // 수정 대상 멤버
                    // 권한 설정
                    String role = memberDto.getRole();
                    if ("LEADER".equals(role) || "MEMBER".equals(role)) {
                        model.addAttribute("role", role);
                    } else {
                        model.addAttribute("role", "GUEST");
                    }
                });
            });

            if (!model.containsAttribute("role")) {
                model.addAttribute("role", "GUEST");
            }
        } else {
            model.addAttribute("role", "GUEST");
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

}
