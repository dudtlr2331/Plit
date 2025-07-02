package com.plit.FO.clan;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clan")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;
    private ClanEntity clan;
    private MultipartFile imageFile;

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
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uploadDir = "uploads/";
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();

                // 업로드 폴더 없으면 생성
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                // 실제 파일 저장
                imageFile.transferTo(new File(uploadDir + fileName));

                // 클라이언트 접근 경로로 설정
                clan.setImageUrl("/upload/" + fileName);

            } catch (IOException e) {
                e.printStackTrace();
                clan.setImageUrl("/uploads/default.png"); // 실패 시 기본 이미지
            }
        } else {
            clan.setImageUrl("/uploads/default.png"); // 업로드 안 했을 때 기본 이미지
        }

        clanService.createClan(clan);
        return "redirect:/clan";
    }

    // 클랜 상세 조회
    @GetMapping("/{id}")
    public String viewClan(@PathVariable Long id, Model model) {
        ClanEntity clan = clanService.getClanById(id);
        model.addAttribute("clan", clan);

        // [임시로 하드코딩] 추후 로그인 연결 시 로직 변경
        String role = "LEADER"; // 또는 MEMBER, GUEST
        model.addAttribute("role", role);

        return "fo/clan/clan-detail";
    }


}