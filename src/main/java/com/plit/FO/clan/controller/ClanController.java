package com.plit.FO.clan.controller;

import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.service.ClanService;
import com.plit.FO.user.UserService;
import com.plit.FO.user.UserDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/clan")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;
    private final UserService userService;

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    // ✅ 클랜 목록 조회
    @GetMapping
    public String listClans(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String tier,
                            Model model) {
        List<ClanEntity> clans = clanService.searchClansByKeywordAndTier(keyword, tier);
        model.addAttribute("clans", clans);
        model.addAttribute("tier", tier);
        return "fo/clan/clan-list";
    }

    // ✅ 클랜 등록 처리 (POST)
    @PostMapping("/register")
    public String registerClan(@ModelAttribute ClanEntity clan,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        // 로그인 안 되어 있으면 로그인 페이지로
        if (principal == null) {
            return "redirect:/login";
        }

        // 로그인된 사용자 정보 가져오기
        String userId = principal.getName();
        Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);
        if (optionalUser.isEmpty()) {
            return "redirect:/login";
        }

        UserDTO loginUser = optionalUser.get();

        // ✅ URL 형식 체크
        String urlRegex = "^(https?://).+";

        boolean hasValidKakao = clan.getKakaoLink() != null &&
                !clan.getKakaoLink().isBlank() &&
                clan.getKakaoLink().matches(urlRegex);

        boolean hasValidDiscord = clan.getDiscordLink() != null &&
                !clan.getDiscordLink().isBlank() &&
                clan.getDiscordLink().matches(urlRegex);

        // ❌ 유효하지 않으면 → 경고 메시지와 함께 리디렉션
        if (!hasValidKakao && !hasValidDiscord) {
            redirectAttributes.addFlashAttribute("clan", clan); // 입력값 유지
            return "redirect:/clan#openModal"; // ✅ 모달 자동 열기!
        }

        // 👑 리더 ID 설정
        clan.setLeaderId(loginUser.getUserSeq().longValue());

        // 🖼️ 이미지 업로드
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                imageFile.transferTo(new File(dir, fileName));
                clan.setImageUrl("/uploads/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                clan.setImageUrl("/images/default.png");
            }
        } else {
            clan.setImageUrl("/images/default.png");
        }

        // 💾 클랜 저장
        clanService.createClan(clan);

        return "redirect:/clan";
    }

    // ✅ 클랜 상세 보기
    @GetMapping("/{id}")
    public String viewClan(@PathVariable Long id, Model model, Principal principal) {
        ClanEntity clan = clanService.getClanById(id);
        model.addAttribute("clan", clan);

        String role = "GUEST";

        if (principal != null) {
            String userId = principal.getName();
            Optional<UserDTO> optionalUser = userService.getUserByUserId(userId);

            if (optionalUser.isPresent()) {
                UserDTO loginUser = optionalUser.get();

                if (clan.getLeaderId().equals(loginUser.getUserSeq().longValue())) {
                    role = "LEADER";
                } else if (clanService.isMember(id, loginUser.getUserSeq().longValue())) {
                    role = "MEMBER";
                }
            }
        }

        model.addAttribute("role", role);
        return "fo/clan/clan-detail";
    }
}