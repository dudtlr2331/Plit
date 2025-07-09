package com.plit.FO.mypage.controller;

import com.plit.FO.qna.service.QnaService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final QnaService qnaService;
    private final UserService userService;

    @GetMapping({"", "/"})
    public String showMypage(@AuthenticationPrincipal Object principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        // 로그인 여부 확인
        if (principal == null) {
            redirectAttributes.addFlashAttribute("popup", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        // 사용자 ID 추출
        String userId = null;
        if (principal instanceof User user) {
            userId = user.getUsername();
        } else if (principal instanceof DefaultOAuth2User oAuth) {
            Map<String, Object> kakaoAccount =
                    (Map<String, Object>) oAuth.getAttributes().get("kakao_account");
            userId = (String) kakaoAccount.get("email");
            if (userId == null) {
                userId = "kakao_" + oAuth.getName(); // fallback
            }
        }

        // 사용자 정보 조회
        UserDTO loginUser = userService.findByUserId(userId);
        if (loginUser == null) {
            redirectAttributes.addFlashAttribute("popup", "사용자 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("viewSection", "account");
        return "fo/mypage/mypage";
    }

    @GetMapping("/qna")
    public String showQnaTab(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) userId = 1L; // 테스트용 fallback

        model.addAttribute("viewSection", "qna");
        model.addAttribute("viewMode", "list");
        model.addAttribute("questions", qnaService.getMyQuestions(userId));
        return "fo/mypage/mypage";
    }
}
