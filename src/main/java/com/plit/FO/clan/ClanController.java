package com.plit.FO.clan;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import com.plit.FO.user.UserDTO;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clan")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    // 클랜 목록 조회
    @GetMapping
    public String listClans(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String tier,
                            Model model) {
        List<ClanEntity> clans = clanService.searchClansByKeywordAndTier(keyword, tier);
        model.addAttribute("clans", clans);
        model.addAttribute("tier", tier);
        return "fo/clan/clan-list";
    }

    // 클랜 등록 폼 (GET)
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("clan", new ClanEntity());
        return "fo/clan/create";
    }

    // 클랜 등록 처리 (POST)
    @PostMapping("/register")
    public String registerClan(@ModelAttribute ClanEntity clan,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               HttpSession session) {

        // 세션에서 로그인한 유저 정보 가져오기
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        System.out.println("현재 로그인 유저: " + loginUser);

        if (loginUser == null) {
            System.out.println("로그인 유저 세션이 null, 로그인 페이지로 리다이렉트");
            return "redirect:/login";
        }

        // 클랜 리더 ID 설정
        clan.setLeaderId(loginUser.getUserSeq().longValue());

        // 이미지 업로드 처리
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // 고유한 파일명 생성
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

                // 업로드 디렉토리 생성
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                // 파일 저장
                imageFile.transferTo(new File(dir, fileName));

                // 클랜 이미지 URL 설정
                clan.setImageUrl("/uploads/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                clan.setImageUrl("/images/default.png"); // 실패 시 기본 이미지로 설정
            }
        } else {
            clan.setImageUrl("/images/default.png"); // 이미지 업로드 안 했을 경우
        }

        clanService.createClan(clan);

        return "redirect:/clan";
    }

    // 클랜 상세 조회
    @GetMapping("/{id}")
    public String viewClan(@PathVariable Long id, Model model, HttpSession session) {
        ClanEntity clan = clanService.getClanById(id);
        model.addAttribute("clan", clan);

        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        String role = "GUEST";

        if (loginUser != null) {
            if (clan.getLeaderId().equals(loginUser.getUserSeq().longValue())) {
                role = "LEADER";
            } else if (clanService.isMember(id, loginUser.getUserSeq().longValue())) {
                role = "MEMBER";
            }
        }

        model.addAttribute("role", role);
        return "fo/clan/clan-detail";
    }
}