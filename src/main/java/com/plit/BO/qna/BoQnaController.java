package com.plit.BO.qna;

import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.service.QnaService;
import com.plit.FO.user.UserEntity;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bo/admin/qna")
public class BoQnaController {

    private final QnaService qnaService;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    @GetMapping("/list")
    public String list(@RequestParam(value = "type", defaultValue = "ALL") String type,
                       Model model,
                       HttpSession session) {

        // 로그인 여부 판단은 세션에서 직접 꺼내거나, SecurityContextHolder에서 꺼냄
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            // 민석이 만든 시스템이라면 userDetails에 user_auth 정보를 따로 저장했는지 체크 필요
            // ex) userDetails.getAuthorities() 또는 커스텀 필드
            // 여기서는 사용자 권한 정보 못 쓰는 구조이니 생략
        } else {
            // 익명 사용자면 차단
            return "redirect:/login"; // 또는 권한 없음 페이지
        }

        // 그냥 관리자만 들어오게 막고 싶은 경우엔 URL 접근을 시큐리티에서 제어하고
        // 컨트롤러에선 따로 체크 안 하는 게 가장 깔끔

        List<QnaEntity> questions = qnaService.getQuestionsByType(type);
        model.addAttribute("questions", questions);
        model.addAttribute("type", type);
        return "bo/admin/qna/all_qna";
    }

    // ✅ 답변 폼 보기
    @GetMapping("/answer/{id}")
    public String answerForm(@PathVariable Long id, Model model) {
        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        return "bo/admin/qna/answer";
    }

    // ✅ 답변 저장 처리
    @PostMapping("/answer/{id}")
    public String submitAnswer(@PathVariable Long id,
                               @RequestParam("answer") String answer) {
        qnaService.saveAnswer(id, answer);
        return "redirect:/bo/admin/qna/list";
    }

    // ✅ 파일 다운로드 기능
    @GetMapping("/download/{file}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String file) {
        String filePath = uploadDir + File.separator + file;
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedFileName = java.net.URLEncoder.encode(file, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}