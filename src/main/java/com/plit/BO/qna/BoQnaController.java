package com.plit.BO.qna;

import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.service.QnaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/bo/admin/qna")
@RequiredArgsConstructor
public class BoQnaController {

    private final QnaService qnaService;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    // ✅ 전체 문의 목록
    @GetMapping("/list")
    public String showAllQuestions(HttpSession session, Model model) {
        if (!hasAdminPermission(session)) return "redirect:/login";

        List<QnaEntity> allQuestions = qnaService.getAllQuestions();
        model.addAttribute("questions", allQuestions);
        return "bo/admin/qna/all_qna";
    }

    // ✅ 미처리 문의 목록
    @GetMapping("/unprocessed")
    public String showUnprocessedQuestions(HttpSession session, Model model) {
        if (!hasAdminPermission(session)) return "redirect:/login";

        List<QnaEntity> unansweredQuestions = qnaService.getUnansweredQuestions();
        model.addAttribute("questions", unansweredQuestions);
        return "bo/admin/qna/unprocessed_qna";
    }

    // ✅ 답변 폼 페이지
    @GetMapping("/answer/{id}")
    public String showAnswerForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!hasAdminPermission(session)) return "redirect:/login";

        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        return "bo/admin/qna/answer";
    }

    // ✅ 답변 저장 처리
    @PostMapping("/answer/{id}")
    public String saveAnswer(@PathVariable Long id,
                             @RequestParam("answer") String answer,
                             HttpSession session) {
        if (!hasAdminPermission(session)) return "redirect:/login";

        qnaService.saveAnswer(id, answer);
        return "redirect:/bo/admin/qna/list";
    }

    // ✅ 파일 다운로드
    @GetMapping("/download/{fileName}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ 채팅 팝업 진입
    @GetMapping("/chat/inquiry/open/{roomId}")
    public String openAdminChat(@PathVariable String roomId, HttpSession session, Model model) {
        if (!hasAdminPermission(session)) return "redirect:/login";

        Object userId = session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);
        return "bo/admin/qna/admchat";
    }

    // ✅ 단독 채팅 화면
    @GetMapping("/admchat")
    public String showAdminChat(HttpSession session) {
        if (!hasAdminPermission(session)) return "redirect:/login";
        return "bo/admin/qna/admchat";
    }

    // ✅ 권한 체크 유틸 (null-safe)
    private boolean hasAdminPermission(HttpSession session) {
        Object authObj = session.getAttribute("userAuth");
        if (authObj instanceof String auth) {
            return "admin".equals(auth) || "master".equals(auth);
        }
        return false;
    }
}