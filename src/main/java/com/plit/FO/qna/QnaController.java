package com.plit.FO.qna;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/fo/mypage/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    // 문의 작성 폼 보여주기
    @GetMapping("/write")
    public String showWriteForm(Model model) {
        model.addAttribute("qnaDTO", new QnaDTO()); // 폼 바인딩용 객체
        return "fo/mypage/qna/write"; // templates 경로
    }

    // 문의 등록 처리
    @PostMapping("/write")
    public String writeQna(@ModelAttribute @Valid QnaDTO dto, BindingResult bindingResult, HttpSession session, RedirectAttributes ra, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("qnaDTO", dto);
            return "fo/mypage/qna/write";
        }
        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/fo/login";
        }

        qnaService.saveQuestion(dto, userId);
        return "redirect:/fo/mypage/qna/list";
    }

    // 문의 삭제 처리
    @PostMapping("/delete/{id}")
    public String deleteQna(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes ra) {

        Long userId = 1L;
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/fo/login";
        }

        qnaService.deleteQna(id, userId);
        return "redirect:/fo/mypage/qna/list";
    }

    // 내 문의 내역 리스트
    @GetMapping("/list")
    public String getMyQuestions(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = 1L; // 테스트용
//        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/fo/login";
        }

        List<QnaEntity> myQuestions = qnaService.getMyQuestions(userId);
        model.addAttribute("questions", myQuestions);
        return "fo/mypage/qna/list";
    }

    @GetMapping("view/{id}")
    public String viewQna(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = 1L; // 테스트용
        //        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/fo/login";
        }

        QnaEntity qna = qnaService.findById(id);
        if (!qna.getUserId().equals(userId)) {
            ra.addFlashAttribute("error", "본인만 열람 가능합니다.");
            return "redirect:/fo/mypage/qna/list";
        }
        model.addAttribute("qna", qna);
        return "fo/mypage/qna/view";

    }
}