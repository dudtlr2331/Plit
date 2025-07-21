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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                       Model model,
                       RedirectAttributes ra) {
        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
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

        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "QnA 목록 조회 중 오류가 발생했습니다.");
            return "redirect:/";
        }
    }

    @GetMapping("/answer/{id}")
    public String answerForm(@AuthenticationPrincipal User user,
                             @PathVariable Long id,
                             Model model,
                             RedirectAttributes ra) {

        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
                return "redirect:/401";
            }

            QnaEntity qna = qnaService.findById(id);
            model.addAttribute("qna", qna);
            model.addAttribute("loginUser", loginUser);
            return "bo/admin/qna/personal_qna_answer";

        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "문의 상세 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/bo/admin/qna/list";
        }
    }

    @PostMapping("/answer/{id}")
    public String submitAnswer(@AuthenticationPrincipal User user,
                               @PathVariable Long id,
                               @RequestParam("answer") String answer,
                               Model model,
                               RedirectAttributes ra) {

        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
                return "redirect:/401";
            }

            qnaService.saveAnswer(id, answer);
            return "redirect:/bo/admin/qna/list";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bo/admin/qna/list";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "답변 저장 중 오류가 발생했습니다.");
            return "redirect:/bo/admin/qna/list";
        }
    }

    @GetMapping("/view/{id}")
    public String viewAnsweredQna(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  Model model,
                                  RedirectAttributes ra) {

        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
                return "redirect:/401";
            }

            QnaEntity qna = qnaService.findById(id);
            model.addAttribute("qna", qna);
            model.addAttribute("loginUser", loginUser);
            return "bo/admin/qna/personal_qna_view";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bo/admin/qna/list";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "QnA 상세 조회 중 오류가 발생했습니다.");
            return "redirect:/bo/admin/qna/list";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteQna(@AuthenticationPrincipal User user,
                            @PathVariable Long id,
                            RedirectAttributes ra) {

        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
                return "redirect:/401";
            }

            qnaService.deleteById(id);
            return "redirect:/bo/admin/qna/list";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bo/admin/qna/list";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "QnA 삭제 중 오류가 발생했습니다.");
            return "redirect:/bo/admin/qna/list";
        }
    }

    @PostMapping("/hard-delete/{id}")
    public String hardDelete(@AuthenticationPrincipal User user,
                             @PathVariable Long id,
                             RedirectAttributes ra) {

        try {
            if (user == null) throw new IllegalStateException("로그인이 필요합니다.");

            UserDTO loginUser = userService.findByUserId(user.getUsername());
            if (loginUser == null) throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");

            if (!"ADMIN".equals(loginUser.getUserAuth()) && !"MASTER".equals(loginUser.getUserAuth())) {
                return "redirect:/401";
            }

            qnaService.hardDelete(id);
            return "redirect:/bo/admin/qna/list?type=DELETED";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bo/admin/qna/list?type=DELETED";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "QnA 완전 삭제 중 오류가 발생했습니다.");
            return "redirect:/bo/admin/qna/list?type=DELETED";
        }
    }

    @GetMapping("/download/{file}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String file) {
        try {
            String filePath = Paths.get(uploadDir, file).toString();
            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build(); // 404
            }

            String encodedFileName = URLEncoder.encode(file, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}