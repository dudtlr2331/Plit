package com.plit.BO.qna;

import com.plit.FO.qna.entity.QnaEntity;
import com.plit.FO.qna.service.QnaService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bo/admin/qna")
public class BoQnaController {

    private final QnaService qnaService;
    private final UserService userService;

    @Value("${custom.upload-path.qna}")
    private String uploadDir;

    @GetMapping("/list")
    public String list(@AuthenticationPrincipal User user,
                       @RequestParam(value = "type", defaultValue = "ALL") String type,
                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                       Model model) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return "redirect:/login";
        if (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth())) {
            return "redirect:/401";
        }

        List<QnaEntity> questions = "DELETED".equalsIgnoreCase(type)
                ? qnaService.getDeletedQuestions()
                : qnaService.getQuestionsByType(type);

        if ("oldest".equals(sort)) {
            questions.sort(java.util.Comparator.comparing(QnaEntity::getAskedAt));
        } else {
            questions.sort(java.util.Comparator.comparing(QnaEntity::getAskedAt).reversed());
        }

        model.addAttribute("questions", questions);
        model.addAttribute("type", type);
        model.addAttribute("sort", sort);
        model.addAttribute("loginUser", loginUser);
        return "bo/admin/qna/personal_qna";
    }

    @GetMapping("/answer/{id}")
    public String answerForm(@AuthenticationPrincipal User user,
                             @PathVariable Long id,
                             Model model) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return "redirect:/login";
        if (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth())) {
            return "redirect:/401";
        }

        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        model.addAttribute("loginUser", loginUser);
        return "bo/admin/qna/personal_qna_answer";
    }

    @PostMapping("/answer/{id}")
    public String submitAnswer(@AuthenticationPrincipal User user,
                               @PathVariable Long id,
                               @RequestParam("answer") String answer,
                               Model model) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return "redirect:/login";
        if (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth())) {
            return "redirect:/401";
        }

        qnaService.saveAnswer(id, answer);
        return "redirect:/bo/admin/qna/list";
    }

    @GetMapping("/view/{id}")
    public String viewAnsweredQna(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  Model model) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return "redirect:/login";
        if (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth())) {
            return "redirect:/401";
        }

        QnaEntity qna = qnaService.findById(id);
        model.addAttribute("qna", qna);
        model.addAttribute("loginUser", loginUser);
        return "bo/admin/qna/personal_qna_view";
    }

    @PostMapping("/delete/{id}")
    public String deleteQna(@AuthenticationPrincipal User user,
                            @PathVariable Long id) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null || (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth()))) {
            return "redirect:/401";
        }

        qnaService.deleteById(id);
        return "redirect:/bo/admin/qna/list";
    }

    @PostMapping("/hard-delete/{id}")
    public String hardDelete(@AuthenticationPrincipal User user,
                             @PathVariable Long id) {

        if (user == null) return "redirect:/login";

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null || (!"admin".equals(loginUser.getUserAuth()) && !"master".equals(loginUser.getUserAuth()))) {
            return "redirect:/401";
        }

        qnaService.hardDelete(id);
        return "redirect:/bo/admin/qna/list?type=DELETED";
    }

    @GetMapping("/download/{file}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String file) {
        String filePath = Paths.get(uploadDir, file).toString();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedFileName = URLEncoder.encode(file, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}