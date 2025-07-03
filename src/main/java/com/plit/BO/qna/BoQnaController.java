package com.plit.BO.qna;

import com.plit.FO.qna.QnaEntity;
import com.plit.FO.qna.QnaService;
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
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/bo/qna")
@RequiredArgsConstructor
public class BoQnaController {

    private final QnaService qnaService;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    // 전체 문의 목록
    @GetMapping("/list")
    public String showAllQuestions(HttpSession session, Model model) {
        String auth = "admin"; // 테스트용
//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) return "redirect:/fo/login";

        List<QnaEntity> allQuestions = qnaService.getAllQuestions();
        model.addAttribute("questions", allQuestions);

        // 테스트용 채팅방 목록 (실제 서비스에서는 DB 또는 세션 기반으로 대체)
        List<Map<String, Object>> pendingRooms = new ArrayList<>();
        Map<String, Object> testRoom = new HashMap<>();
        testRoom.put("inquiryRoomId", "admin-room");
        testRoom.put("userId", "user1");
        testRoom.put("createdAt", LocalDateTime.now());
        pendingRooms.add(testRoom);
        model.addAttribute("pendingRooms", pendingRooms);

        return "bo/qna/list";
    }

    // 답변 폼
    @GetMapping("/answer/{id}")
    public String showAnswerForm(@PathVariable Long id, HttpSession session, Model model) {
        String auth = "admin"; // 테스트용
//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) return "redirect:/fo/login";

        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        return "bo/qna/answer";
    }

    // 답변 저장 처리
    @PostMapping("/answer/{id}")
    public String saveAnswer(@PathVariable Long id, @RequestParam("answer") String answer, HttpSession session) {
        String auth = "admin"; // 테스트용
//        String auth = (String) session.getAttribute("userAuth");
//        if (!"admin".equals(auth)) return "redirect:/fo/login";

        qnaService.saveAnswer(id, answer);
        return "redirect:/bo/qna/list";
    }

    // 첨부파일 다운로드
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 관리자 채팅 화면 (팝업으로 오픈됨)
    @GetMapping("/chat/inquiry/open/{roomId}")
    public String openAdminChat(@PathVariable String roomId, Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", "admin"); // 테스트용
        // model.addAttribute("userId", session.getAttribute("userId")); // 실사용 시
        return "bo/qna/admchat";
    }

    // 관리자 채팅 HTML 단독 진입 (개별 테스트용)
    @GetMapping("/admchat")
    public String showAdminChat() {
        return "bo/qna/admchat";
    }
}