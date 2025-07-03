package com.plit.FO.mypage;

import com.plit.FO.qna.QnaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final QnaService qnaService;

    // 마이페이지 기본 탭 (계정 정보)
    @GetMapping({"", "/"})
    public String showMypage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loginUser") == null) {
            redirectAttributes.addFlashAttribute("popup", "로그인이 필요합니다.");
            return "redirect:/main";
        }

        model.addAttribute("viewSection", "account");
        return "fo/mypage/mypage";
    }

    @GetMapping("/qna")
    public String showQnaTab(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) userId = 1L;

        model.addAttribute("viewSection", "qna");
        model.addAttribute("viewMode", "list");
        model.addAttribute("questions", qnaService.getMyQuestions(userId));
        return "fo/mypage/mypage";
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loginUser") == null) {
            redirectAttributes.addFlashAttribute("popup", "로그인이 필요합니다.");
            return "redirect:/fo/main";
        }
        return "fo/mypage/mypage";
    }
}