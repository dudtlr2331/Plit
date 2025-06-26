package com.plit.BO.qna;

import com.plit.FO.qna.QnaEntity;
import com.plit.FO.qna.QnaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/bo/qna")
@RequiredArgsConstructor
public class BoQnaController {

    private final QnaService qnaService;

    // 전체 문의 목록
    @GetMapping("/list")
    public String showAllQuestions(HttpSession session, Model model) {

        String auth = "admin"; // 테스트용

//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) {
//            return "redirect:/fo/login";
//        }

        List<QnaEntity> allQuestions = qnaService.getAllQuestions();
        model.addAttribute("questions", allQuestions);
        return "bo/qna/list";
    }

    // 답변 폼
    @GetMapping("/answer/{id}")
    public String showAnswerForm(@PathVariable Long id, HttpSession session, Model model) {

        String auth = "admin"; // 테스트용
//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) {
//            return "redirect:/fo/login";
//        }

        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        return "bo/qna/answer";
    }

    // 답변 저장 처리
    @PostMapping("/answer/{id}")
    public String saveAnswer(@PathVariable Long id, @RequestParam("answer") String answer, HttpSession session) {

        String auth = "admin"; // 테스트용
//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) {
//            return "redirect:/fo/login";
//        }

        qnaService.saveAnswer(id, answer);
        return "redirect:/bo/qna/list";
    }
}