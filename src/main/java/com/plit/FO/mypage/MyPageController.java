package com.plit.FO.mypage;

import com.plit.FO.qna.service.QnaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final QnaService qnaService;

    // 마이페이지 기본 탭 (계정 정보)
    @GetMapping({"", "/"})
    public String showMypage(Model model) {
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
}